package ge.hackerman.gree.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import java.net.Inet4Address

/** The IPv4 subnet the phone is currently attached to. */
data class LocalSubnet(
    val hosts: List<String>,
    val broadcast: String?,
    /** Human-readable network, e.g. "192.168.0.0/24". */
    val cidr: String,
)

/**
 * Works out which addresses are worth probing. Gree units answer unicast reliably but
 * broadcast only sometimes, so we need the full host list, not just 255.
 */
object LanScanner {

    /** Sweeping more than this is slow enough to feel broken, so we skip it. */
    private const val MAX_HOSTS = 512

    fun currentSubnet(context: Context): LocalSubnet? {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = manager.activeNetwork ?: return null
        val properties = manager.getLinkProperties(network) ?: return null

        val address = properties.linkAddresses.firstOrNull {
            it.address is Inet4Address && !it.address.isLoopbackAddress
        } ?: return null

        return expand(address)
    }

    private fun expand(link: LinkAddress): LocalSubnet? {
        val prefix = link.prefixLength
        if (prefix !in 16..30) return null

        val octets = link.address.address
        val ip = octets.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val network = ip and mask
        val broadcast = network or mask.inv().and(0xFFFFFFFFL)

        val total = broadcast - network - 1
        if (total <= 0 || total > MAX_HOSTS) return null

        val hosts = ((network + 1) until broadcast)
            .filter { it != ip }
            .map { it.toIpString() }

        return LocalSubnet(
            hosts = hosts,
            broadcast = broadcast.toIpString(),
            cidr = "${network.toIpString()}/$prefix",
        )
    }

    private fun Long.toIpString(): String =
        "${(this shr 24) and 0xFF}.${(this shr 16) and 0xFF}.${(this shr 8) and 0xFF}.${this and 0xFF}"
}
