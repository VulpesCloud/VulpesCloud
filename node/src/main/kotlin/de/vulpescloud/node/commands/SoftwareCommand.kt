package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.serversoftware.impl.FoliaDownloader
import de.vulpescloud.node.serversoftware.impl.PaperDownloader
import de.vulpescloud.node.serversoftware.impl.PurpurDownloader
import de.vulpescloud.node.serversoftware.impl.VelocityDownloader
import de.vulpescloud.node.terminal.COLOR.VULPES_ORANGE
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.suggestion.Suggestions
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

    @Command("software list")
    fun listSoftwares(source: CommandSource) {
        source.sendMessage("Available Server Softwares:")
        NodeCoroutineScope.launch {
            val maxDisplayNameLength = downloaders.maxOf { it.displayName.length }
            val allVersions =
                downloaders.flatMap { downloader ->
                    downloader.getAvailableVersions().map { downloader to it }
                }
            val maxVersionLength = allVersions.maxOf { it.second.version.length }

            allVersions.forEach { (downloader, software) ->
                source.sendMessage(
                    "<gray> - <color:$VULPES_ORANGE>${downloader.displayName.padEnd(maxDisplayNameLength)}</color> Version: <yellow>${
                        software.version.padEnd(
                            maxVersionLength
                        )
                    }</yellow> Type: ${software.type}</gray>"
                )
            }
        }
    }
}
