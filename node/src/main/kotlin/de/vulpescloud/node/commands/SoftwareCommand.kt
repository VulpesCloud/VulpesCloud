package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.serversoftware.impl.FoliaDownloader
import de.vulpescloud.node.serversoftware.impl.PaperDownloader
import de.vulpescloud.node.serversoftware.impl.PurpurDownloader
import de.vulpescloud.node.serversoftware.impl.VelocityDownloader
import de.vulpescloud.node.terminal.COLOR.VULPES_ORANGE
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
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
