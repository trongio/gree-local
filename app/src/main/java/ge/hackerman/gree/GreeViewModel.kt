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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Faster while a single unit is on screen, slower when polling the whole list. */
private const val CONTROL_POLL_MS = 3000L
private const val LIST_POLL_MS = 5000L

/** Everything the UI needs about one unit, whether or not it has answered yet. */
data class DeviceUi(
    val device: GreeDevice,
    val state: GreeState?,
    val online: Boolean,
)

class GreeViewModel(application: Application) : AndroidViewModel(application) {

    private val client = GreeClient()
    private val store = DeviceStore(application)

    private val _states = MutableStateFlow<Map<String, GreeState>>(emptyMap())
    private val _reachable = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _selectedMac = MutableStateFlow<String?>(null)
    val selectedMac: StateFlow<String?> = _selectedMac.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _hostCount = MutableStateFlow(0)
    val hostCount: StateFlow<Int> = _hostCount.asStateFlow()

    private val _subnetLabel = MutableStateFlow("LAN ONLY")
    val subnetLabel: StateFlow<String> = _subnetLabel.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val devices: StateFlow<List<GreeDevice>> = store.devices

    /**
     * Devices joined with their last poll result. This has to be a flow the UI collects:
     * reading the state maps directly from a composable never subscribes to them, so the
     * screen would only catch up when something else happened to trigger recomposition.
     */
    val deviceUis: StateFlow<List<DeviceUi>> =
        combine(store.devices, _states, _reachable) { devices, states, reachable ->
            devices.map { device ->
                DeviceUi(
                    device = device,
                    state = states[device.mac],
                    // Unknown counts as online until a poll actually fails.
                    online = reachable[device.mac] ?: true,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedUi: StateFlow<DeviceUi?> =
        combine(deviceUis, _selectedMac) { uis, mac -> uis.firstOrNull { it.device.mac == mac } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var pollJob: Job? = null

    init {
        refreshSubnetLabel()
        startPolling()
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun refreshSubnetLabel() {
        val subnet = LanScanner.currentSubnet(getApplication())
        _hostCount.value = subnet?.hosts?.size ?: 0
        _subnetLabel.value = subnet?.cidr?.let { "$it · LAN ONLY" } ?: "NO WI-FI · LAN ONLY"
    }

    // ── polling ────────────────────────────────────────────────────────────────

    /**
     * One loop serves both screens: the control screen needs one unit refreshed often,
     * the list needs every unit refreshed slowly. Re-selecting restarts it.
     */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val focus = _selectedMac.value
                val targets = if (focus != null) {
                    devices.value.filter { it.mac == focus }
                } else {
                    devices.value
                }

                if (targets.isNotEmpty()) refreshAll(targets)
                delay(if (focus != null) CONTROL_POLL_MS else LIST_POLL_MS)
            }
        }
    }

    private suspend fun refreshAll(targets: List<GreeDevice>) = coroutineScope {
        targets.map { device ->
            async {
                runCatching { client.status(device.ip, device.mac, device.key) }
                    .onSuccess { state ->
                        _states.value = _states.value + (device.mac to state)
                        _reachable.value = _reachable.value + (device.mac to true)
                    }
                    .onFailure {
                        _reachable.value = _reachable.value + (device.mac to false)
                    }
            }
        }.awaitAll()
    }

    // ── discovery ──────────────────────────────────────────────────────────────

    fun scan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            _progress.value = 0f
            try {
                refreshSubnetLabel()
                val subnet = LanScanner.currentSubnet(getApplication())
                if (subnet == null) {
                    _message.value = "Connect to Wi-Fi first"
                    return@launch
                }

                val found = client.discover(subnet.hosts, subnet.broadcast) { _progress.value = it }
                if (found.isEmpty()) {
                    _message.value = "No Gree units found on this network"
                    return@launch
                }

                var bound = 0
                for (unit in found) {
                    val existing = devices.value.firstOrNull { it.mac == unit.mac }
                    runCatching { client.bind(unit.ip, unit.mac) }.onSuccess { key ->
                        store.upsert(
                            GreeDevice(
                                mac = unit.mac,
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
                refreshAll(devices.value)
            } catch (e: Exception) {
                _message.value = e.message ?: "Scan failed"
            } finally {
                _scanning.value = false
                _progress.value = 0f
            }
        }
    }

    fun addByIp(ip: String) {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            _progress.value = 0f
            try {
                val found = client.discover(listOf(ip), broadcast = null, windowMs = 2000) {
                    _progress.value = it
                }
                val unit = found.firstOrNull()
                if (unit == null) {
                    _message.value = "Nothing answered at $ip"
                    return@launch
                }
                val key = client.bind(unit.ip, unit.mac)
                val device = GreeDevice(unit.mac, unit.name, unit.ip, key)
                store.upsert(device)
                _message.value = "Added ${device.name}"
                refreshAll(listOf(device))
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not add $ip"
            } finally {
                _scanning.value = false
                _progress.value = 0f
            }
        }
    }

    // ── control ────────────────────────────────────────────────────────────────

    fun select(mac: String?) {
        _selectedMac.value = mac
        startPolling()
    }

    /**
     * Applies the change locally first so the UI reacts on touch, then reconciles with
     * whatever the unit reports back.
     */
    fun send(mac: String, vararg options: Pair<String, Int>) {
        val device = devices.value.firstOrNull { it.mac == mac } ?: return
        val updates = options.toMap()

        _states.value[mac]?.let { current ->
            _states.value = _states.value + (mac to GreeState(current.raw + updates))
        }

        viewModelScope.launch {
            runCatching { client.command(device.ip, device.mac, device.key, updates) }
                .onFailure {
                    _reachable.value = _reachable.value + (mac to false)
                    _message.value = "Command failed"
                }
            refreshAll(listOf(device))
        }
    }

    fun togglePower(mac: String) {
        val on = _states.value[mac]?.power ?: false
        send(mac, ge.hackerman.gree.protocol.Gree.POWER to if (on) 0 else 1)
    }

    fun rename(mac: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) store.rename(mac, trimmed)
    }

    fun forget(mac: String) {
        if (_selectedMac.value == mac) select(null)
        store.remove(mac)
        _states.value = _states.value - mac
        _reachable.value = _reachable.value - mac
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
