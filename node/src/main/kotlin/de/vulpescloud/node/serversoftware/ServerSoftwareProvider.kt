package de.vulpescloud.node.serversoftware

import de.vulpescloud.node.NodeCoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ServerSoftwareProvider {
    private val downloaders = mutableMapOf<String, ServerSoftwareDownloader>()
    private var allowDownloaderAdding = true
    private val logger = LoggerFactory.getLogger(ServerSoftwareProvider::class.java)

    fun registerDownloader(downloader: ServerSoftwareDownloader) {
        if (allowDownloaderAdding) {
            downloaders[downloader.id] = downloader
            return
        }
        logger.error("Cannot add downloader ${downloader.id} after startup!")
    }

    fun getDownloader(id: String) = downloaders[id]

    fun getFromDisplayName(displayName: String) =
        downloaders.values.firstOrNull { it.displayName == displayName }

    fun downloaders() = downloaders.values

    fun lock() {
        allowDownloaderAdding = false
    }

    fun triggerReCache() {
        downloaders.values.forEach {
            NodeCoroutineScope.launch {
                it.getAvailableVersions(true)
            }
        }
    }
}
