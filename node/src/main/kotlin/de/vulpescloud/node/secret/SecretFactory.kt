package de.vulpescloud.node.secret

import java.nio.file.Files
import java.nio.file.Path

class SecretFactory {
    fun loadOrCreateSecret(path: Path) : String {
        if (!Files.exists(path)) {
            return create(path)
        }

        return Files.readString(path)
    }

    private fun create(path: Path): String {
        val secret = SecretGenerator.generate()

        if (!Files.exists(path)) {
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, secret)
        }

        return secret
    }
}