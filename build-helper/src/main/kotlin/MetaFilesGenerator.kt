import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.Path


fun generateCheckSums(destPath: String) {
    val destinationPath = Path(destPath)
    val checksumJson = JSONObject()
    checksumJson.put("api", getFileChecksum(destinationPath.resolve("vulpescloud-api.jar").toFile()))
    checksumJson.put("bridge", getFileChecksum(destinationPath.resolve("vulpescloud-bridge.jar").toFile()))
    checksumJson.put("connector", getFileChecksum(destinationPath.resolve("vulpescloud-connector.jar").toFile()))
    checksumJson.put("node", getFileChecksum(destinationPath.resolve("vulpescloud-node.jar").toFile()))
    checksumJson.put("wrapper", getFileChecksum(destinationPath.resolve("vulpescloud-wrapper.jar").toFile()))

    Files.writeString(destinationPath.resolve("checksums.json"), checksumJson.toString(4))
}

private fun getFileChecksum(file: File): String {
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