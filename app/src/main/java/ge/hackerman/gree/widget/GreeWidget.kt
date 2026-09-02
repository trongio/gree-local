package ge.hackerman.gree.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ge.hackerman.gree.MainActivity
import ge.hackerman.gree.R
import ge.hackerman.gree.data.DeviceStore
import ge.hackerman.gree.data.GreeDevice
import ge.hackerman.gree.data.StateCache
import ge.hackerman.gree.protocol.Gree
import ge.hackerman.gree.protocol.GreeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val ACTION_POWER = "ge.hackerman.gree.widget.POWER"
private const val ACTION_STEP = "ge.hackerman.gree.widget.STEP"
private const val EXTRA_DELTA = "delta"

/**
 * A one-unit home screen control. Renders instantly from [StateCache] and refreshes
 * behind that, because a widget cannot block its first frame on a UDP round trip.
 */
class GreeWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { GreeWidget.render(context, manager, it) }

        val pending = goAsync()
        scope.launch {
            try {
                widgetIds.forEach { GreeWidget.refresh(context, it) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        WidgetTargets.clear(context, widgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_POWER && intent.action != ACTION_STEP) return

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pending = goAsync()
        scope.launch {
            try {
                GreeWidget.act(context, widgetId, intent.action!!, intent.getIntExtra(EXTRA_DELTA, 0))
            } finally {
                pending.finish()
            }
        }
    }
}

object GreeWidget {

    private val client = GreeClient()

    /** Repaints every placed widget from cache, without touching the network. */
    fun pushAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, GreeWidgetProvider::class.java))
        ids.forEach { render(context, manager, it) }
    }

    fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_gree)
        val device = deviceFor(context, widgetId)

        if (device == null) {
            views.setTextViewText(R.id.w_name, context.getString(R.string.app_name))
            views.setTextViewText(R.id.w_temp, "--")
            views.setTextViewText(R.id.w_sub, "TAP TO SET UP")
            views.setOnClickPendingIntent(R.id.w_open, openApp(context, widgetId))
            manager.updateAppWidget(widgetId, views)
            return
        }

        val cached = StateCache.get(context, device.mac)
        val state = cached?.state
        val power = state?.power == true
        val online = cached?.online ?: true

        views.setTextViewText(R.id.w_name, device.name)
        views.setTextViewText(
            R.id.w_temp,
            when {
                state == null -> "--"
                power -> "${state.targetTemp}°"
                else -> "Off"
            },
        )

        val room = state?.roomTemp?.let { "ROOM $it°C" } ?: "ROOM —"
        views.setTextViewText(
            R.id.w_sub,
            when {
                !online -> "$room · OFFLINE"
                state == null -> "TAP TO OPEN"
                power -> "$room · ${state.mode.label.uppercase()}"
                else -> "$room · STANDBY"
            },
        )
        views.setTextColor(
            R.id.w_sub,
            context.getColor(if (online) R.color.w_ink2 else R.color.w_warm),
        )
        views.setTextColor(
            R.id.w_temp,
            context.getColor(if (power) R.color.w_ink else R.color.w_ink2),
        )

        // The power pill is the only filled control, so it reads as the primary action.
        views.setInt(
            R.id.w_power,
            "setBackgroundResource",
            if (power) R.drawable.widget_btn_on else R.drawable.widget_btn,
        )
        views.setInt(
            R.id.w_power,
            "setColorFilter",
            context.getColor(if (power) R.color.w_on_accent else R.color.w_ink),
        )
        views.setInt(R.id.w_minus, "setColorFilter", context.getColor(R.color.w_ink))
        views.setInt(R.id.w_plus, "setColorFilter", context.getColor(R.color.w_ink))

        views.setOnClickPendingIntent(R.id.w_open, openApp(context, widgetId))
        views.setOnClickPendingIntent(R.id.w_power, broadcast(context, widgetId, ACTION_POWER, 0))
        views.setOnClickPendingIntent(R.id.w_minus, broadcast(context, widgetId, ACTION_STEP, -1))
        views.setOnClickPendingIntent(R.id.w_plus, broadcast(context, widgetId, ACTION_STEP, +1))

        manager.updateAppWidget(widgetId, views)
    }

    /** Polls the unit and repaints. Safe to call off the main thread only. */
    suspend fun refresh(context: Context, widgetId: Int) {
        val device = deviceFor(context, widgetId) ?: return
        runCatching { client.status(device.ip, device.mac, device.key) }
            .onSuccess { StateCache.put(context, device.mac, it, online = true) }
            .onFailure { StateCache.setOffline(context, device.mac) }

        AppWidgetManager.getInstance(context)?.let { render(context, it, widgetId) }
    }

    suspend fun act(context: Context, widgetId: Int, action: String, delta: Int) {
        val device = deviceFor(context, widgetId) ?: return
        val current = StateCache.get(context, device.mac)?.state

        val options = when (action) {
            ACTION_POWER -> mapOf(Gree.POWER to if (current?.power == true) 0 else 1)
            ACTION_STEP -> {
                val base = current?.targetTemp ?: return
                val next = (base + delta).coerceIn(Gree.MIN_TEMP, Gree.MAX_TEMP)
                if (next == base) return
                mapOf(Gree.SET_TEMP to next)
            }
            else -> return
        }

        // Write through first so the widget reacts on tap rather than after the round trip.
        current?.let {
            StateCache.put(
                context,
                device.mac,
                ge.hackerman.gree.protocol.GreeState(it.raw + options),
                online = true,
            )
            AppWidgetManager.getInstance(context)?.let { m -> render(context, m, widgetId) }
        }

        runCatching { client.command(device.ip, device.mac, device.key, options) }
            .onFailure { StateCache.setOffline(context, device.mac) }
        refresh(context, widgetId)
    }

    private fun deviceFor(context: Context, widgetId: Int): GreeDevice? {
        val mac = WidgetTargets.mac(context, widgetId) ?: return null
        return DeviceStore(context).devices.value.firstOrNull { it.mac == mac }
    }

    private fun openApp(context: Context, widgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            widgetId * 10,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun broadcast(context: Context, widgetId: Int, action: String, delta: Int): PendingIntent {
        val intent = Intent(context, GreeWidgetProvider::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(EXTRA_DELTA, delta)
        }
        // Request codes must differ per button, or they share one PendingIntent.
        val code = widgetId * 10 + when {
            action == ACTION_POWER -> 1
            delta < 0 -> 2
            else -> 3
        }
        return PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
