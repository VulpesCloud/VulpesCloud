package de.vulpescloud.node.serversoftware.impl

import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.node.serversoftware.ServerSoftwareDownloader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

object PaperDownloader : ServerSoftwareDownloader {
    override val id: String = "paper"
    override val displayName: String = "Paper"

    private const val BASE_API_URL = "https://fill.papermc.io/v3"
    private val logger = LoggerFactory.getLogger("PaperDownloader")

    private val availableVersionsCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build<String, List<ServerSoftware>>()

    private val downloadUrlCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build<String, URI>()

    private val latestVersionCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build<String, ServerSoftware>()

    override suspend fun downloadSoftware(version: String) {
        val start = System.currentTimeMillis()
        val downloadUrl = getDownloadUrl(version)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(downloadUrl.toString())
            .header("User-Agent", "VulpesCloud-Node/1.0")
            .build()

        val downloadFileName = downloadUrl.path.substringAfterLast('/')
        logger.info("Downloading $downloadFileName ...")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val file = File("local/versions/$downloadFileName")

            if (file.exists()) {
                logger.info("$downloadFileName already exists, skipping download.")
                return@use
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

    override suspend fun getLatestVersionPath(version: String): Path {
        val latestVersion = getLatestVersion(version)
        return Path("local/versions/paper-${latestVersion.version}-${latestVersion.build}.jar")
    }

    override suspend fun getDownloadUrl(version: String): URI {
        val cached = downloadUrlCache.getIfPresent(version)
        if (cached != null) return cached

        val apiUrl = "$BASE_API_URL/projects/paper/versions/$version/builds/latest"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "VulpesCloud-Node/1.0")
            .build()

        val result = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val responseBody = response.body.string()

            val jResponse = JSONObject(responseBody)

            val downloadUrl = jResponse
                .getJSONObject("downloads")
                .getJSONObject("server:default")
                .getString("url")
            URI(downloadUrl)
        }

        downloadUrlCache.put(version, result)
        return result
    }

    override suspend fun getAvailableVersions(refreshList: Boolean): List<ServerSoftware> {
        if (refreshList) {
            availableVersionsCache.invalidate("all")
        }

        val cached = availableVersionsCache.getIfPresent("all")
        if (cached != null) return cached

        val result = pullAvailableVersions()
        availableVersionsCache.put("all", result)
        return result
    }

    override suspend fun getLatestVersion(version: String?): ServerSoftware {
        val cacheKey = version ?: "latest"
        val cached = latestVersionCache.getIfPresent(cacheKey)
        if (cached != null) return cached

        val apiUrl = "$BASE_API_URL/projects/paper/versions"
        val allVersions = getAvailableVersions()

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "VulpesCloud-Node/1.0")
            .build()

        val result = if (version == null) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val responseBody = response.body.string()

                val jResponse = JSONObject(responseBody)

                val versions = jResponse.getJSONArray("versions")

                if (versions.length() == 0) throw Exception("No versions found")

                val latestVersion = versions.getJSONObject(0).getJSONObject("version")

                val downloadUrl = getDownloadUrl(latestVersion.getString("id"))

                val build = downloadUrl.path.substringAfterLast('-').substringBefore('.').toIntOrNull()

                ServerSoftware(
                    name = "Paper",
                    version = latestVersion.getString("id"),
                    build = build ?: 1,
                    url = downloadUrl.toString(),
                    pluginDir = "plugins",
                    type = SoftwareType.SERVER
                )
            }
        } else {
            val matchingVersion = allVersions.find { it.version == version }
            if (matchingVersion == null) throw Exception("No version found for Paper with version $version")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val responseBody = response.body.string()

                val jResponse = JSONObject(responseBody)

                val versions = jResponse.getJSONArray("versions")

                if (versions.length() == 0) throw Exception("No versions found")

                val downloadUrl = getDownloadUrl(version)

                val build = downloadUrl.path.substringAfterLast('-').substringBefore('.').toIntOrNull()

                ServerSoftware(
                    name = "Paper",
                    version = version,
                    build = build ?: 1,
                    url = downloadUrl.toString(),
                    pluginDir = "plugins",
                    type = SoftwareType.SERVER
                )
            }
        }

        latestVersionCache.put(cacheKey, result)
        return result
    }

    private suspend fun pullAvailableVersions(): List<ServerSoftware> {
        val apiUrl = "$BASE_API_URL/projects/paper/versions"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "VulpesCloud-Node/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val responseBody = response.body.string()

            val jResponse = JSONObject(responseBody)

            val versions = jResponse.getJSONArray("versions")

            val softwareList = mutableListOf<ServerSoftware>()

            for (i in 0 until versions.length()) {
                val version = versions.getJSONObject(i).getJSONObject("version")

                val downloadUrl = getDownloadUrl(version.getString("id"))

                val build = downloadUrl.path.substringAfterLast('-').substringBefore('.').toIntOrNull()

                val software = ServerSoftware(
                    name = "Paper",
                    version = version.getString("id"),
                    build = build ?: 1,
                    url = downloadUrl.toString(),
                    pluginDir = "plugins",
                    type = SoftwareType.SERVER
                )

                softwareList.add(software)
            }

            return softwareList
        }
    }
}