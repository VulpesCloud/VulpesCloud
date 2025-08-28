package de.vulpescloud.node.serversoftware.impl

import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.node.serversoftware.ServerSoftwareDownloader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URI

object CanvasDownloader : ServerSoftwareDownloader {
    private const val BASE_API_URL = "https://canvasmc.io/api/v2"
    private val logger = LoggerFactory.getLogger("CanvasDownloader")

    override suspend fun downloadSoftware(version: String) {
        val start = System.currentTimeMillis()
        val downloadUrl = getDownloadUrl(version)

        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val request = Request.Builder()
            .url(downloadUrl.toString())
            .header("User-Agent", "VulpesCloud-Node/1.0")
            .build()

        val downloadFileName = "canvas-$version-${downloadUrl.path.split("/")[3].replace(" ", "")}.jar"
        logger.info("Downloading $downloadFileName ...")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val file = File("local/versions/$downloadFileName")

            if (file.exists()) {
                logger.info("$downloadFileName already exists, skipping download.")
                return
            }

            val fileBytes = response.body.bytes()

            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }

            FileOutputStream(file).use { it.write(fileBytes) }

            val durationMs = System.currentTimeMillis() - start
            logger.info(
                "Finished downloading $downloadFileName (${file.length() / 1000000}mb) in ${if (durationMs >= 1000) "${durationMs / 1000}s" else "${durationMs}ms}"}"
            )
        }
    }

    override suspend fun getDownloadUrl(version: String): URI {
        val builds = getAllBuilds()

        val build = builds.filter { it.getString("minecraftVersion") == version }
            .maxByOrNull { it.getInt("buildNumber") }
            ?: throw Exception("No build found for version $version")

        return URI.create(build.getString("downloadUrl"))
    }

    private fun getAllBuilds(): List<JSONObject> {
        val apiUrl = "$BASE_API_URL/builds?experimental=true"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "VulpesCloud-Node/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val responseBody = response.body.string()

            val jResponse = JSONArray(responseBody)

            val builds = mutableListOf<JSONObject>()

            jResponse.forEach {
                it as JSONObject
                builds.add(it)
            }

            return builds
        }
    }

    override suspend fun getAvailableVersions(): List<ServerSoftware> {
        val builds = getAllBuilds()

        val versions = builds.map { it.getString("minecraftVersion") }.distinct().sortedByDescending {
            val parts = it.split(".").map { part -> part.toIntOrNull() ?: 0 }
            parts[0] * 1_000_000 + (parts.getOrNull(1) ?: 0) * 1_000 + (parts.getOrNull(2) ?: 0)
        }
        return versions.map { version ->
            val downloadUrl = getDownloadUrl(version)
            ServerSoftware(
                name = "Canvas",
                version = version,
                url = downloadUrl.toString(),
                pluginDir = "plugins",
                type = SoftwareType.SERVER
            )
        }
    }

    override suspend fun getLatestVersion(): ServerSoftware {
        val getCurrentVersionApiUrl = "$BASE_API_URL/builds/latest?experimental=true"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(getCurrentVersionApiUrl)
            .header("User-Agent", "VulpesCloud-Node/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            val responseBody = response.body.string()

            val jResponse = JSONObject(responseBody)

            return ServerSoftware(
                name = "Canvas",
                version = jResponse.getString("minecraftVersion"),
                url = jResponse.getString("downloadUrl"),
                pluginDir = "plugins",
                type = SoftwareType.SERVER
            )
        }
    }
}