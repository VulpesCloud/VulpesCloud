package de.vulpescloud.node.serversoftware.impl

import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.serversoftware.ServerSoftware
import de.vulpescloud.api.serversoftware.SoftwareType
import de.vulpescloud.node.serversoftware.ServerSoftwareDownloader
import de.vulpescloud.node.utils.PropertyUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.slf4j.LoggerFactory

object VelocityDownloader : ServerSoftwareDownloader {
    override val id: String = "velocity"
    override val displayName: String = "Velocity"

    private const val BASE_API_URL = "https://fill.papermc.io/v3"
    private val logger = LoggerFactory.getLogger("VelocityDownloader")

    private val availableVersionsCache = Caffeine.newBuilder().build<String, List<ServerSoftware>>()

    private val downloadUrlCache = Caffeine.newBuilder().build<String, URI>()

    private val latestVersionCache = Caffeine.newBuilder().build<String, ServerSoftware>()

    override suspend fun downloadSoftware(version: String) {
        val start = System.currentTimeMillis()
        val downloadUrl = getDownloadUrl(version)

        val client = OkHttpClient()
        val request =
            Request.Builder()
                .url(downloadUrl.toString())
                .header("User-Agent", "VulpesCloud-Node/1.0")
                .build()

        val downloadFileName = downloadUrl.path.substringAfterLast('/')

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val file = File("local/versions/$downloadFileName")

            if (file.exists()) {
                return@use
            }
            logger.info("Downloading $downloadFileName ...")

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
        return Path("local/versions/velocity-${latestVersion.version}-${latestVersion.build}.jar")
    }

    override suspend fun getDownloadUrl(version: String): URI {
        if (PropertyUtils.isMoreSoftwareLogging())
            logger.info("VelocityDownloader> Getting download URL for version $version")
        val cached = downloadUrlCache.getIfPresent(version)
        if (version == "4.0.0")
            throw UnsupportedOperationException("Velocity version 4.0.0 is not supported")
        if (cached != null) return cached

        val apiUrl = "$BASE_API_URL/projects/velocity/versions/$version/builds/latest"

        val client = OkHttpClient()

        val request =
            Request.Builder().url(apiUrl).header("User-Agent", "VulpesCloud-Node/1.0").build()

        val start = System.nanoTime()
        val result =
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val responseBody = response.body.string()

                val jResponse = JSONObject(responseBody)

                val downloadUrl =
                    jResponse
                        .getJSONObject("downloads")
                        .getJSONObject("server:default")
                        .getString("url")
                URI(downloadUrl)
            }
        val duration = (System.nanoTime() - start) / 1_000_000.0
        if (PropertyUtils.isSoftwareTiming()) {
            logger.info("VelocityDownloader> getDownloadUrl($version) took ${duration}ms")
        }

        downloadUrlCache.put(version, result)
        return result
    }

    override suspend fun getAvailableVersions(refreshList: Boolean): List<ServerSoftware> {
        if (PropertyUtils.isMoreSoftwareLogging())
            logger.info("VelocityDownloader> Getting available versions (refreshList=$refreshList)")
        if (refreshList) {
            availableVersionsCache.invalidate("all")
        }

        val cached = availableVersionsCache.getIfPresent("all")
        if (cached != null) return cached

        val start = System.nanoTime()
        val result = pullAvailableVersions()
        val duration = (System.nanoTime() - start) / 1_000_000.0
        if (PropertyUtils.isSoftwareTiming()) {
            logger.info("VelocityDownloader> getAvailableVersions() took ${duration}ms")
        }
        availableVersionsCache.put("all", result)
        return result
    }

    override suspend fun getLatestVersion(version: String?): ServerSoftware {
        if (PropertyUtils.isMoreSoftwareLogging())
            logger.info("VelocityDownloader> Getting latest version for ${version ?: "latest"}")
        val cacheKey = version ?: "latest"
        val cached = latestVersionCache.getIfPresent(cacheKey)
        if (cached != null) return cached

        val apiUrl = "$BASE_API_URL/projects/velocity/versions"
        val allVersions = getAvailableVersions()

        val client = OkHttpClient()

        val request =
            Request.Builder().url(apiUrl).header("User-Agent", "VulpesCloud-Node/1.0").build()

        val start = System.nanoTime()
        val result =
            if (version == null) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    val responseBody = response.body.string()

                    val jResponse = JSONObject(responseBody)

                    val versions = jResponse.getJSONArray("versions")

                    if (versions.length() == 0) throw Exception("No versions found")

                    val latestVersion = versions.getJSONObject(0).getJSONObject("version")

                    val downloadUrl =
                        if (latestVersion.getString("id") == "4.0.0" || latestVersion.getString("id") == "3.5.0") {
                            getDownloadUrl(
                                versions.getJSONObject(1).getJSONObject("version").getString("id")
                            )
                        } else {
                            getDownloadUrl(latestVersion.getString("id"))
                        }

                    val build =
                        downloadUrl.path.substringAfterLast('-').substringBefore('.').toIntOrNull()

                    ServerSoftware(
                        name = "Velocity",
                        version = latestVersion.getString("id"),
                        build = build ?: 1,
                        url = downloadUrl.toString(),
                        pluginDir = "plugins",
                        type = SoftwareType.PROXY,
                    )
                }
            } else {
                val matchingVersion = allVersions.find { it.version == version }
                if (matchingVersion == null)
                    throw Exception("No version found for Velocity with version $version")

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    val responseBody = response.body.string()

                    val jResponse = JSONObject(responseBody)

                    val versions = jResponse.getJSONArray("versions")

                    if (versions.length() == 0) throw Exception("No versions found")

                    val downloadUrl =
                        if (version == "4.0.0" || version == "3.5.0") {
                            URI("")
                        } else {
                            getDownloadUrl(version)
                        }

                    val build =
                        downloadUrl.path.substringAfterLast('-').substringBefore('.').toIntOrNull()

                    ServerSoftware(
                        name = "Velocity",
                        version = version,
                        build = build ?: 1,
                        url = downloadUrl.toString(),
                        pluginDir = "plugins",
                        type = SoftwareType.PROXY,
                    )
                }
            }
        val duration = (System.nanoTime() - start) / 1_000_000.0
        if (PropertyUtils.isSoftwareTiming()) {
            logger.info(
                "VelocityDownloader> getLatestVersion(${version ?: "latest"}) took ${duration}ms"
            )
        }

        latestVersionCache.put(cacheKey, result)
        return result
    }

    private suspend fun pullAvailableVersions(): List<ServerSoftware> {
        if (PropertyUtils.isMoreSoftwareLogging())
            logger.info("VelocityDownloader> Pulling available versions from API")
        val apiUrl = "$BASE_API_URL/projects/velocity/versions"

        val client = OkHttpClient()

        val request =
            Request.Builder().url(apiUrl).header("User-Agent", "VulpesCloud-Node/1.0").build()

        val start = System.nanoTime()
        val result =
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val responseBody = response.body.string()

                val jResponse = JSONObject(responseBody)

                val versions = jResponse.getJSONArray("versions")

                val softwareList = mutableListOf<ServerSoftware>()

                for (i in 0 until versions.length()) {
                    val version = versions.getJSONObject(i).getJSONObject("version")

                    if (version.getString("id") == "4.0.0" || version.getString("id") == "3.5.0") {
                        continue
                    } else {
                        val build =
                            getDownloadUrl(version.getString("id"))
                                .path
                                .substringAfterLast('-')
                                .substringBefore('.')
                                .toIntOrNull()

                        val software =
                            ServerSoftware(
                                name = "Velocity",
                                version = version.getString("id"),
                                build = build ?: 1,
                                url = getDownloadUrl(version.getString("id")).toString(),
                                pluginDir = "plugins",
                                type = SoftwareType.PROXY,
                            )

                        softwareList.add(software)
                    }
                }

                softwareList
            }
        val duration = (System.nanoTime() - start) / 1_000_000.0
        if (PropertyUtils.isSoftwareTiming()) {
            logger.info("VelocityDownloader> pullAvailableVersions() took ${duration}ms")
        }
        return result
    }
}
