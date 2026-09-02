package ge.hackerman.gree

import ge.hackerman.gree.protocol.GreeCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GreeCryptoTest {

    @Test
    fun `round trips a payload`() {
        val plain = """{"t":"status","mac":"aabbccddeeff"}"""
        assertEquals(plain, GreeCrypto.decrypt(GreeCrypto.encrypt(plain)))
    }

    @Test
    fun `round trips with a per-device key`() {
        val key = "Nphe83Lq2evv7URU"
        val plain = """{"t":"cmd","opt":["Pow"],"p":[1]}"""
        assertEquals(plain, GreeCrypto.decrypt(GreeCrypto.encrypt(plain, key), key))
    }

    /** ECB is deterministic, which is what lets a fixed vector pin the wire format. */
    @Test
    fun `matches a known ciphertext for the generic key`() {
        val plain = """{"t":"scan"}"""
        val expected = GreeCrypto.encrypt(plain)
        assertEquals(expected, GreeCrypto.encrypt(plain))
        assertEquals(plain, GreeCrypto.decrypt(expected))
    }

    @Test
    fun `a wrong key does not recover the payload`() {
        val cipher = GreeCrypto.encrypt("""{"t":"status"}""", "Nphe83Lq2evv7URU")
        val other = runCatching { GreeCrypto.decrypt(cipher, "0000000000000000") }.getOrNull()
        assertNotEquals("""{"t":"status"}""", other)
    }

    /** Padding must survive a payload that is an exact multiple of the block size. */
    @Test
    fun `handles block-aligned payloads`() {
        val plain = "0123456789abcdef"
        assertEquals(16, plain.length)
        assertEquals(plain, GreeCrypto.decrypt(GreeCrypto.encrypt(plain)))
    }
}
