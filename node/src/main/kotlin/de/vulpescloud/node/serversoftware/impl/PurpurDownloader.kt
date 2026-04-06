package de.vulpescloud.node.serversoftware.impl

import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.node.serversoftware.ServerSoftwareDownloader
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.slf4j.LoggerFactory

object PurpurDownloader : ServerSoftwareDownloader {
    override val id: String = "purpur"
    override val displayName: String = "Purpur"

    private const val BASE_API_URL = "https://api.purpurmc.org/v2"
    private val logger = LoggerFactory.getLogger("PurpurDownloader")

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

        val client = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()

        val request =
            Request.Builder()
                .url(downloadUrl.toString())
                .header("User-Agent", "VulpesCloud-Node/1.0")
                .build()

        val downloadFileName =
            "purpur-$version-${downloadUrl.path.split("/")[4].replace(" ", "")}.jar"
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

    override suspend fun getDownloadUrl(version: String): URI {
        val cached = downloadUrlCache.getIfPresent(version)
        if (cached != null) return cached

        val latestBuildUrl = "$BASE_API_URL/purpur/$version"

        val client = OkHttpClient()

        val buildRequest = Request.Builder().url(latestBuildUrl).build()

        val result = client.newCall(buildRequest).execute().use { buildResponse ->
            if (!buildResponse.isSuccessful) throw Exception("Unexpected code $buildResponse")

            val buildResponseBody = buildResponse.body.string()
            val jBuildResponse = JSONObject(buildResponseBody)

            val build = jBuildResponse.getJSONObject("builds").getString("latest")

            URI("$BASE_API_URL/purpur/$version/$build/download")
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

    override suspend fun getLatestVersionPath(version: String): Path {
        val latest = getLatestVersion(version)
        val fileName = "purpur-${latest.version}-${latest.build}.jar"
        return Path("local/versions/$fileName")
    }

    override suspend fun getLatestVersion(version: String?): ServerSoftware {
        val cacheKey = version ?: "latest"
        val cached = latestVersionCache.getIfPresent(cacheKey)
        if (cached != null) return cached

        val getCurrentVersionApiUrl = "$BASE_API_URL/purpur"
        val allVersions = getAvailableVersions()

        val client = OkHttpClient()

        val versionRequest =
            Request.Builder()
                .url(getCurrentVersionApiUrl)
                .header("User-Agent", "VulpesCloud-Node/1.0")
                .build()

        val result = if (version == null) {
            client.newCall(versionRequest).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val responseBody = response.body.string()

                val jVersionResponse = JSONObject(responseBody)

                val latestVersion = jVersionResponse.getJSONObject("metadata").getString("current")

                val latestBuildUrl = "$BASE_API_URL/purpur/$latestVersion"

                val buildRequest =
                    Request.Builder()
                        .url(latestBuildUrl)
                        .header("User-Agent", "VulpesCloud-Node/1.0")
                        .build()

                client.newCall(buildRequest).execute().use { buildResponse ->
                    if (!buildResponse.isSuccessful)
                        throw Exception("Unexpected code $buildResponse")

                    ServerSoftware(
                        name = "Purpur",
                        version = latestVersion,
                        build = jVersionResponse.getJSONObject("builds").getInt("latest"),
                        url = getDownloadUrl(latestVersion).toString(),
                        pluginDir = "plugins",
                        type = SoftwareType.SERVER,
                    )
                }
            }
        } else {
            val matchingVersion = allVersions.find { it.version == version }
            if (matchingVersion == null)
                throw Exception("No version found for Purpur with version $version")

            val latestBuildUrl = "$BASE_API_URL/purpur/$version"

            val buildRequest =
                Request.Builder()
                    .url(latestBuildUrl)
                    .header("User-Agent", "VulpesCloud-Node/1.0")
                    .build()

            client.newCall(buildRequest).execute().use { buildResponse ->
                if (!buildResponse.isSuccessful) throw Exception("Unexpected code $buildResponse")

                val jBuildResponse = JSONObject(buildResponse.body.string())

                ServerSoftware(
                    name = "Purpur",
                    version = version,
                    build = jBuildResponse.getJSONObject("builds").getInt("latest"),
                    url = getDownloadUrl(version).toString(),
                    pluginDir = "plugins",
                    type = SoftwareType.SERVER,
                )
            }
        }

        latestVersionCache.put(cacheKey, result)
        return result
    }

    private suspend fun pullAvailableVersions(): List<ServerSoftware> {
        val apiUrl = "$BASE_API_URL/purpur"

        val client = OkHttpClient()

        val request = Request.Builder().url(apiUrl).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val responseBody = response.body.string()

            val jResponse = JSONObject(responseBody)

            val versions = jResponse.getJSONArray("versions").toList().reversed()

            return versions.map {
                val version = it as String
                val downloadUrl = getDownloadUrl(version)
                ServerSoftware(
                    name = "Purpur",
                    version = version,
                    build = downloadUrl.path.split("/")[4].toInt(),
                    url = downloadUrl.toString(),
                    pluginDir = "plugins",
                    type = SoftwareType.SERVER,
                )
            }
        }
    }
}
