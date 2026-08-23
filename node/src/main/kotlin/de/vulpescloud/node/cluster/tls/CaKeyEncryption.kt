package de.vulpescloud.node.cluster.tls

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import kotlinx.serialization.Serializable

/**
 * The CA private key is stored in the database, but never in plaintext.
 * It's encrypted with a key derived from the cluster secret, so only
 * nodes that already know the cluster secret (i.e. are legitimately
 * configured) can ever decrypt it - the DB alone is not enough.
 */
object CaKeyEncryption {

    private const val PBKDF2_ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    @Serializable
    data class EncryptedBlob(
        val saltB64: String,
        val ivB64: String,
        val ciphertextB64: String,
    )

    fun encrypt(plaintextPem: String, clusterSecret: String): EncryptedBlob {
        val salt = randomBytes(16)
        val iv = randomBytes(GCM_IV_LENGTH_BYTES)
        val key = deriveKey(clusterSecret, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintextPem.toByteArray(Charsets.UTF_8))

        return EncryptedBlob(
            saltB64 = Base64.getEncoder().encodeToString(salt),
            ivB64 = Base64.getEncoder().encodeToString(iv),
            ciphertextB64 = Base64.getEncoder().encodeToString(ciphertext),
        )
    }

    fun decrypt(blob: EncryptedBlob, clusterSecret: String): String {
        val salt = Base64.getDecoder().decode(blob.saltB64)
        val iv = Base64.getDecoder().decode(blob.ivB64)
        val ciphertext = Base64.getDecoder().decode(blob.ciphertextB64)
        val key = deriveKey(clusterSecret, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    private fun deriveKey(clusterSecret: String, salt: ByteArray): SecretKeySpec {
        val spec: KeySpec = PBEKeySpec(clusterSecret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun randomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }
}
