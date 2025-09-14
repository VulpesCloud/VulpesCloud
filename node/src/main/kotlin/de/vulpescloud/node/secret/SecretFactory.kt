package de.vulpescloud.node.secret

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.util.*
import kotlin.io.path.*

class SecretFactory(private val random: SecureRandom = SecureRandom()) {
    fun loadOrCreateSecret(path: Path, bytes: Int = 32): String {
        if (path.exists() && path.isRegularFile()) {
            return path.readText(StandardCharsets.UTF_8).trim()
        }
        val secret = generateBase64(bytes)
        writeAtomic(path, secret + "\n")
        return secret
    }

    private fun generateBase64(len: Int): String {
        val buf = ByteArray(len)
        random.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    private fun writeAtomic(target: Path, content: String) {
        target.parent?.createDirectories()
        val tmp = target.resolveSibling(target.fileName.toString() + ".tmp")
        tmp.writeText(content, StandardCharsets.UTF_8)
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
