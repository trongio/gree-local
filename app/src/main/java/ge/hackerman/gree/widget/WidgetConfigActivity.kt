package ge.hackerman.gree.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.data.DeviceStore
import ge.hackerman.gree.data.GreeDevice
import ge.hackerman.gree.ui.theme.AlbertSans
import ge.hackerman.gree.ui.theme.GreeTheme
import ge.hackerman.gree.ui.theme.PlexMono
import ge.hackerman.gree.ui.theme.Gree as GreeColors

/**
 * Asks which unit a newly placed widget should control. With a single unit there is
 * nothing to ask, so it commits and closes without ever being seen.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Cancelled unless we explicitly say otherwise, so backing out drops the widget.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val devices = DeviceStore(this).devices.value
        if (devices.size == 1) {
            commit(devices.first())
            return
        }

        enableEdgeToEdge()
        setContent {
            GreeTheme {
                Picker(devices = devices, onPick = ::commit)
            }
        }
    }

    private fun commit(device: GreeDevice) {
        WidgetTargets.set(this, widgetId, device.mac)
        AppWidgetManager.getInstance(this)?.let { GreeWidget.render(this, it, widgetId) }
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}

@Composable
private fun Picker(devices: List<GreeDevice>, onPick: (GreeDevice) -> Unit) {
    val c = GreeColors.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (devices.isEmpty()) "No units yet" else "Which unit?",
            fontFamily = AlbertSans,
            fontWeight = FontWeight.W600,
            fontSize = 26.sp,
            letterSpacing = (-0.01).em,
            color = c.ink,
        )

        if (devices.isEmpty()) {
            Text(
                "Open Gree Local and scan your network first, then add the widget again.",
                fontFamily = AlbertSans,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = c.ink2,
            )
            return@Column
        }

        devices.forEach { device ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .clickable { onPick(device) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    device.name,
                    fontFamily = AlbertSans,
                    fontWeight = FontWeight.W600,
                    fontSize = 18.sp,
                    color = c.ink,
                )
                Text(
                    "${device.ip} · ${device.mac}",
                    fontFamily = PlexMono,
                    fontSize = 11.sp,
                    color = c.ink2,
                )
            }
        }
    }
}
