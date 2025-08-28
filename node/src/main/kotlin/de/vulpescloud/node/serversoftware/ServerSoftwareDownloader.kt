package de.vulpescloud.node.serversoftware

import de.vulpescloud.api.serversoftware.ServerSoftware
import java.net.URI

interface ServerSoftwareDownloader {
    suspend fun downloadSoftware(version: String)

    suspend fun getDownloadUrl(version: String): URI

    suspend fun getAvailableVersions(): List<ServerSoftware>

    suspend fun getLatestVersion(): ServerSoftware
}