package ge.hackerman.gree.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/** A unit that answered a discovery probe but has not been bound yet. */
data class DiscoveredUnit(
    val ip: String,
    val mac: String,
    val name: String,
)

class GreeUnreachableException(ip: String) : Exception("No reply from $ip")

/**
 * Talks the Gree local UDP protocol. Every call is a single request/response
 * round trip on port 7000, so there is no connection to hold open.
 */
class GreeClient(private val timeoutMs: Int = 2500) {

    private companion object {
        /** Fraction of the progress bar spent probing, the rest is the listen window. */
        const val SEND_SHARE = 0.4f
    }

    /**
     * Probes every host in [hosts] plus the broadcast address and collects whatever
     * answers within [windowMs]. Broadcast alone is unreliable: plenty of routers and
     * AP-isolation setups drop it, so we sweep unicast as well and de-duplicate by MAC.
     */
    suspend fun discover(
        hosts: List<String>,
        broadcast: String?,
        windowMs: Long = 3000,
        onProgress: (Float) -> Unit = {},
    ): List<DiscoveredUnit> = withContext(Dispatchers.IO) {
        val probe = JSONObject().put("t", "scan").toString().toByteArray()
        val found = LinkedHashMap<String, DiscoveredUnit>()

        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.soTimeout = 250

            val targets = buildList {
                broadcast?.let { add(it) }
                addAll(hosts)
            }
            targets.forEachIndexed { index, host ->
                runCatching {
                    socket.send(
                        DatagramPacket(probe, probe.size, InetAddress.getByName(host), Gree.PORT),
                    )
                }
                onProgress(SEND_SHARE * (index + 1) / targets.size)
            }

            // Sending is quick; the listen window is most of the wall clock, so the bar
            // keeps moving against elapsed time rather than freezing at the handover.
            val started = System.currentTimeMillis()
            val deadline = started + windowMs
            val buffer = ByteArray(4096)
            while (true) {
                val now = System.currentTimeMillis()
                if (now >= deadline) break
                onProgress(SEND_SHARE + (1f - SEND_SHARE) * (now - started) / windowMs)

                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                val unit = parseScanReply(packet) ?: continue
                found.putIfAbsent(unit.mac, unit)
            }
            onProgress(1f)
        }
        found.values.toList()
    }

    private fun parseScanReply(packet: DatagramPacket): DiscoveredUnit? = runCatching {
        val outer = JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8))
        val body = JSONObject(GreeCrypto.decrypt(outer.getString("pack")))
        if (body.optString("t") != "dev") return null
        val mac = body.optString("mac").ifEmpty { outer.optString("cid") }
        if (mac.isEmpty()) return null
        DiscoveredUnit(
            ip = packet.address.hostAddress ?: return null,
            mac = mac,
            // Units that were never named in the vendor app fall back to a MAC fragment.
            name = body.optString("name").ifEmpty { mac.takeLast(6) },
        )
    }.getOrNull()

    /** Binds this client to a unit and returns the per-device key it issues. */
    suspend fun bind(ip: String, mac: String): String = withContext(Dispatchers.IO) {
        val pack = JSONObject()
            .put("mac", mac)
            .put("t", "bind")
            .put("uid", 0)
            .toString()

        val response = request(
            ip = ip,
            envelope = envelope(mac, GreeCrypto.encrypt(pack), i = 1),
            key = GreeCrypto.GENERIC_KEY,
        )
        response.getString("key")
    }

    suspend fun status(ip: String, mac: String, key: String): GreeState = withContext(Dispatchers.IO) {
        val pack = JSONObject()
            .put("cols", JSONArray(Gree.STATUS_COLUMNS))
            .put("mac", mac)
            .put("t", "status")
            .toString()

        val response = request(ip, envelope(mac, GreeCrypto.encrypt(pack, key)), key)
        val cols = response.getJSONArray("cols")
        val dat = response.getJSONArray("dat")

        val values = buildMap {
            for (index in 0 until cols.length()) {
                // Some firmware returns strings for numeric fields; coerce rather than crash.
                val value = dat.opt(index)
                val number = (value as? Number)?.toInt() ?: value?.toString()?.toIntOrNull()
                if (number != null) put(cols.getString(index), number)
            }
        }
        GreeState(values)
    }

    /** Applies one or more parameter changes and returns the unit's echoed state. */
    suspend fun command(
        ip: String,
        mac: String,
        key: String,
        options: Map<String, Int>,
    ): Boolean = withContext(Dispatchers.IO) {
        if (options.isEmpty()) return@withContext true

        val pack = JSONObject()
            .put("opt", JSONArray(options.keys.toList()))
            .put("p", JSONArray(options.values.toList()))
            .put("t", "cmd")
            .toString()

        val response = request(ip, envelope(mac, GreeCrypto.encrypt(pack, key)), key)
        response.optInt("r", 200) == 200
    }

    private fun envelope(mac: String, pack: String, i: Int = 0): String =
        JSONObject()
            .put("cid", "app")
            .put("i", i)
            .put("t", "pack")
            .put("uid", 0)
            .put("tcid", mac)
            .put("pack", pack)
            .toString()

    /** Sends one datagram and decrypts the reply's inner pack. */
    private fun request(ip: String, envelope: String, key: String): JSONObject {
        val payload = envelope.toByteArray(Charsets.UTF_8)
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            socket.send(
                DatagramPacket(payload, payload.size, InetAddress.getByName(ip), Gree.PORT),
            )

            val buffer = ByteArray(8192)
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (_: SocketTimeoutException) {
                throw GreeUnreachableException(ip)
            }
            val outer = JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8))
            return JSONObject(GreeCrypto.decrypt(outer.getString("pack"), key))
        }
    }
}
