package ge.hackerman.gree.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Persists bound devices in SharedPreferences. The payload is a handful of small
 * records, so a full rewrite on every change is cheaper than any incremental scheme
 * and keeps cold start free of a database init.
 */
class DeviceStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _devices = MutableStateFlow(load())
    val devices: StateFlow<List<GreeDevice>> = _devices.asStateFlow()

    private fun load(): List<GreeDevice> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { GreeDevice.fromJson(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun persist(devices: List<GreeDevice>) {
        val array = JSONArray().apply { devices.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply()
        _devices.value = devices
    }

    /** Adds a device, or updates the stored IP, name and key if the MAC is already known. */
    fun upsert(device: GreeDevice) {
        val current = _devices.value
        val index = current.indexOfFirst { it.mac == device.mac }
        persist(
            if (index >= 0) current.toMutableList().apply { this[index] = device }
            else current + device,
        )
    }

    fun remove(mac: String) {
        persist(_devices.value.filterNot { it.mac == mac })
    }

    fun rename(mac: String, name: String) {
        _devices.value.firstOrNull { it.mac == mac }?.let { upsert(it.copy(name = name)) }
    }

    private companion object {
        const val FILE_NAME = "gree_devices"
        const val KEY_DEVICES = "devices"
    }
}
