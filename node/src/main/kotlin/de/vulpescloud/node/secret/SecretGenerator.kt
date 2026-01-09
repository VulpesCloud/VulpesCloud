package de.vulpescloud.node.secret

import java.security.SecureRandom
import java.util.*

object SecretGenerator {

    fun generate(size: Int = 64): String {
        val random = SecureRandom()
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

}