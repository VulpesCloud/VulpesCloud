package de.vulpescloud.launcher.util

import de.vulpescloud.launcher.VulpesLauncher.Companion.config
import de.vulpescloud.launcher.VulpesLauncher.Companion.GITHUBURL
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.Path

object ChecksumUtil {

    fun getFileChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun returnChecksumJson(): JSONObject {
        return JSONObject(Files.readString(Path("launcher/checksums.json")))
    }

    fun downloadChecksumJson() {
        Files.writeString(
            Path("launcher/checksums.json"),
            String(
                URI(GITHUBURL + config.autoUpdatesBranch() + "/checksums.json")
                .toURL()
                .openStream()
                .readAllBytes()
            )
        )
    }

}