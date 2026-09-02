package ge.hackerman.gree.protocol

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Gree's v1 local protocol wraps every payload in AES-128-ECB with PKCS7 padding.
 *
 * Discovery and binding use a hardcoded key that is the same on every unit; once a
 * client binds, the unit hands back a per-device key used for all later traffic.
 */
object GreeCrypto {

    const val GENERIC_KEY = "a3K8Bx%2r8Y7#xDh"

    private fun cipher(mode: Int, key: String): Cipher =
        Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
            init(mode, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        }

    fun encrypt(plain: String, key: String = GENERIC_KEY): String =
        Base64.getEncoder().encodeToString(
            cipher(Cipher.ENCRYPT_MODE, key).doFinal(plain.toByteArray(Charsets.UTF_8)),
        )

    fun decrypt(payload: String, key: String = GENERIC_KEY): String =
        String(
            cipher(Cipher.DECRYPT_MODE, key).doFinal(Base64.getDecoder().decode(payload)),
            Charsets.UTF_8,
        )
}
