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

package org.vulpesstudios.vulpescloud.node.commands

import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.FoliaDownloader
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.PaperDownloader
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.PurpurDownloader
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.VelocityDownloader
import java.util.stream.Stream

@Suppress("UNUSED")
class SoftwareCommand {

    private val downloaders =
        listOf(
            // CanvasDownloader, //TODO: The Canvas API has 'changed' a bit i think. The response is
            // a bit different then what we expected
            FoliaDownloader,
            PaperDownloader,
            PurpurDownloader,
            VelocityDownloader,
        )

    @Suggestions("softwares")
    fun softwareSuggestions(): Stream<String> {
        return downloaders.stream().map { it.id }
    }

    @Permission("software.list")
    @Command("software list")
    fun listSoftwares(source: CommandSource) {
        source.sendMessage("<gray>Available server softwares:</gray>")
        runBlocking {
            val maxDisplayNameLength = downloaders.maxOf { it.displayName.length }
            val allVersions =
                downloaders.flatMap { downloader ->
                    downloader.getAvailableVersions().map { downloader to it }
                }
            val maxVersionLength = allVersions.maxOf { it.second.version.length }

            allVersions.forEach { (downloader, software) ->
                source.sendMessage(
                    " <dark_gray>»</dark_gray> <gold>${downloader.displayName.padEnd(maxDisplayNameLength)}</gold> <gray>Version:</gray> <white>${
                        software.version.padEnd(
                            maxVersionLength
                        )
                    }</white> <gray>Type:</gray> <white>${software.type}</white>"
                )
            }
        }
    }

    @Permission("software.reCache")
    @Confirmation
    @Command("software reCache")
    fun reCache(source: CommandSource) {
        source.sendMessage("<gray>Re-caching software versions...</gray>")
        Node.instance.serverSoftwareProvider.triggerReCache()
        source.sendMessage("<gray>Re-caching software versions in background...</gray>")
    }
}
