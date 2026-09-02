package ge.hackerman.gree

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ge.hackerman.gree.data.DeviceStore
import ge.hackerman.gree.data.GreeDevice
import ge.hackerman.gree.data.LanScanner
import ge.hackerman.gree.protocol.GreeClient
import ge.hackerman.gree.protocol.GreeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 4000L

class GreeViewModel(application: Application) : AndroidViewModel(application) {

    private val client = GreeClient()
    private val store = DeviceStore(application)

    val devices: StateFlow<List<GreeDevice>> = store.devices

    private val _selected = MutableStateFlow<GreeDevice?>(null)
    val selected: StateFlow<GreeDevice?> = _selected.asStateFlow()

    private val _state = MutableStateFlow<GreeState?>(null)
    val state: StateFlow<GreeState?> = _state.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var pollJob: Job? = null

    fun consumeMessage() {
        _message.value = null
    }

    /**
     * Sweeps the current subnet and binds anything new. Re-binding a device we already
     * know is harmless and refreshes its key, which is what we want if the unit was
     * reset or re-bound by the vendor app in the meantime.
     */
    fun scan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            try {
                val subnet = LanScanner.currentSubnet(getApplication())
                if (subnet == null) {
                    _message.value = "Connect to Wi-Fi first"
                    return@launch
                }

                val found = client.discover(subnet.hosts, subnet.broadcast)
                if (found.isEmpty()) {
                    _message.value = "No Gree units found on this network"
                    return@launch
                }

                var bound = 0
                for (unit in found) {
                    val existing = devices.value.firstOrNull { it.mac == unit.mac }
                    runCatching { client.bind(unit.ip, unit.mac) }
                        .onSuccess { key ->
                            store.upsert(
                                GreeDevice(
                                    mac = unit.mac,
                                    // Keep a name the user already chose.
                                    name = existing?.name ?: unit.name,
                                    ip = unit.ip,
                                    key = key,
                                ),
                            )
                            bound++
                        }
                }
                _message.value = when (bound) {
                    0 -> "Found ${found.size} unit(s) but binding failed"
                    else -> "Found $bound unit(s)"
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Scan failed"
            } finally {
                _scanning.value = false
            }
        }
    }

    /** Adds a unit by IP for networks where the sweep is blocked or the unit is off-subnet. */
    fun addByIp(ip: String) {
        viewModelScope.launch {
            _scanning.value = true
            try {
                val found = client.discover(hosts = listOf(ip), broadcast = null, windowMs = 2000)
                val unit = found.firstOrNull()
                if (unit == null) {
                    _message.value = "Nothing answered at $ip"
                    return@launch
                }
                val key = client.bind(unit.ip, unit.mac)
                store.upsert(GreeDevice(unit.mac, unit.name, unit.ip, key))
                _message.value = "Added ${unit.name}"
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not add $ip"
            } finally {
                _scanning.value = false
            }
        }
    }

    fun select(device: GreeDevice?) {
        pollJob?.cancel()
        _selected.value = device
        _state.value = null
        if (device == null) return

        pollJob = viewModelScope.launch {
            while (true) {
                refresh(device)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun refresh(device: GreeDevice) {
        runCatching { client.status(device.ip, device.mac, device.key) }
            .onSuccess {
                _state.value = it
                _online.value = true
            }
            .onFailure { _online.value = false }
    }

    /**
     * Applies a change optimistically so the UI reacts instantly, then reconciles with
     * whatever the unit actually reports.
     */
    fun send(vararg options: Pair<String, Int>) {
        val device = _selected.value ?: return
        val updates = options.toMap()

        _state.value?.let { current ->
            _state.value = GreeState(current.raw + updates)
        }

        viewModelScope.launch {
            runCatching { client.command(device.ip, device.mac, device.key, updates) }
                .onFailure {
                    _online.value = false
                    _message.value = "Command failed"
                }
            refresh(device)
        }
    }

    fun rename(mac: String, name: String) = store.rename(mac, name)

    fun forget(mac: String) {
        if (_selected.value?.mac == mac) select(null)
        store.remove(mac)
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
