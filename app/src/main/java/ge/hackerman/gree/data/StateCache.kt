package ge.hackerman.gree.data

import android.content.Context
import ge.hackerman.gree.protocol.GreeState
import org.json.JSONObject

/**
 * Last known state per unit, shared between the app and the home screen widget.
 *
 * The widget cannot wait on a UDP round trip before its first frame, so it renders from
 * here and refreshes behind that. The app writes every poll result back so a widget
 * placed next to an open app is not stale.
 */
object StateCache {

    private const val FILE_NAME = "gree_state_cache"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun put(context: Context, mac: String, state: GreeState, online: Boolean) {
        val json = JSONObject().apply {
            state.raw.forEach { (k, v) -> put(k, v) }
            put("_online", online)
            put("_at", System.currentTimeMillis())
        }
        prefs(context).edit().putString(mac, json.toString()).apply()
    }

    fun setOffline(context: Context, mac: String) {
        val existing = prefs(context).getString(mac, null) ?: return
        val json = runCatching { JSONObject(existing) }.getOrNull() ?: return
        json.put("_online", false)
        prefs(context).edit().putString(mac, json.toString()).apply()
    }

    fun get(context: Context, mac: String): CachedState? {
        val raw = prefs(context).getString(mac, null) ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null

        val values = buildMap {
            json.keys().forEach { key ->
                if (!key.startsWith("_")) json.optInt(key).let { put(key, it) }
            }
        }
        return CachedState(
            state = GreeState(values),
            online = json.optBoolean("_online", true),
            updatedAt = json.optLong("_at", 0L),
        )
    }

    fun remove(context: Context, mac: String) {
        prefs(context).edit().remove(mac).apply()
    }
}

data class CachedState(
    val state: GreeState,
    val online: Boolean,
    val updatedAt: Long,
)
