package ge.hackerman.gree

import ge.hackerman.gree.data.LanScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanScannerTest {

    private fun ip(a: Int, b: Int, c: Int, d: Int) =
        byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte())

    @Test
    fun `expands a slash twenty-four the way the real network is shaped`() {
        val subnet = LanScanner.expand(ip(192, 168, 0, 240), 24)
        assertNotNull(subnet)
        assertEquals("192.168.0.255", subnet!!.broadcast)
        assertEquals("192.168.0.0/24", subnet.cidr)
        // 254 usable hosts, minus this device itself.
        assertEquals(253, subnet.hosts.size)
        assertTrue("192.168.0.199" in subnet.hosts)
        assertTrue("192.168.0.1" in subnet.hosts)
    }

    /** Probing our own address is wasted work, and would answer as ourselves. */
    @Test
    fun `excludes the device's own address`() {
        val subnet = LanScanner.expand(ip(192, 168, 0, 240), 24)!!
        assertFalse("192.168.0.240" in subnet.hosts)
    }

    @Test
    fun `never probes the network or broadcast address`() {
        val subnet = LanScanner.expand(ip(10, 0, 5, 7), 24)!!
        assertFalse("10.0.5.0" in subnet.hosts)
        assertFalse("10.0.5.255" in subnet.hosts)
    }

    @Test
    fun `handles a small subnet`() {
        val subnet = LanScanner.expand(ip(192, 168, 1, 5), 29)!!
        assertEquals("192.168.1.0/29", subnet.cidr)
        assertEquals("192.168.1.7", subnet.broadcast)
        assertEquals(5, subnet.hosts.size)
    }

    /** A /16 is 65k probes: slow enough to look broken, so it is refused. */
    @Test
    fun `refuses a sweep too large to finish`() {
        assertNull(LanScanner.expand(ip(10, 0, 0, 1), 16))
    }

    @Test
    fun `refuses prefixes outside the useful range`() {
        assertNull(LanScanner.expand(ip(10, 0, 0, 1), 8))
        assertNull(LanScanner.expand(ip(10, 0, 0, 1), 31))
    }
}
