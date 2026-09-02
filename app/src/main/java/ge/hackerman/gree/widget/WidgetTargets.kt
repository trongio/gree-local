package ge.hackerman.gree.widget

import android.content.Context

/** Which unit each placed widget points at. */
object WidgetTargets {

    private const val FILE_NAME = "gree_widget_targets"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun mac(context: Context, widgetId: Int): String? =
        prefs(context).getString(widgetId.toString(), null)

    fun set(context: Context, widgetId: Int, mac: String) {
        prefs(context).edit().putString(widgetId.toString(), mac).apply()
    }

    fun clear(context: Context, widgetIds: IntArray) {
        prefs(context).edit().apply {
            widgetIds.forEach { remove(it.toString()) }
        }.apply()
    }
}
