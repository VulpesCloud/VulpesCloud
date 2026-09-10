/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.serversoftware

import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope

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
