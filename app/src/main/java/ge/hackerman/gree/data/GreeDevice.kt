package ge.hackerman.gree.data

import org.json.JSONObject

/**
 * A bound unit. [key] is the per-device AES key handed out at bind time, so losing it
 * means re-binding, not re-pairing with any cloud service.
 */
data class GreeDevice(
    val mac: String,
    val name: String,
    val ip: String,
    val key: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("mac", mac)
        .put("name", name)
        .put("ip", ip)
        .put("key", key)

    companion object {
        fun fromJson(json: JSONObject) = GreeDevice(
            mac = json.getString("mac"),
            name = json.getString("name"),
            ip = json.getString("ip"),
            key = json.getString("key"),
        )
    }
}
