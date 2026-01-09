package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.serversoftware.ServerSoftwareDownloader
import de.vulpescloud.node.serversoftware.impl.*
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.suggestion.Suggestions
import java.util.stream.Stream

@Suppress("UNUSED")
class SoftwareCommand {

    private val downloaders =
        mapOf(
            "Canvas" to CanvasDownloader,
            "Folia" to FoliaDownloader,
            "Paper" to PaperDownloader,
            "Purpur" to PurpurDownloader,
            "Velocity" to VelocityDownloader,
        )

    @Suggestions("softwares")
    fun softwareSuggestions(): Stream<String> {
        return downloaders.keys.stream()
    }

    @Command("software list")
    fun listSoftwares(source: CommandSource) {
        source.sendMessage("Available Server Softwares:")
        downloaders.keys.forEach { source.sendMessage(" - $it") }
    }

    @Command("software list <software>")
    fun listSoftwareVersions(
        source: CommandSource,
        @Argument(value = "software", suggestions = "softwares") software: String,
    ) {
        val downloader =
            downloaders.entries.find { it.key.equals(software, true) }?.value
                ?: run {
                    source.sendMessage("Software $software not found!")
                    return
                }

        NodeCoroutineScope.launch {
            source.sendMessage("Fetching versions for $software...")
            try {
                val versions = downloader.getAvailableVersions()
                source.sendMessage("Available versions for $software:")
                versions.forEach { source.sendMessage(" - ${it.version}") }
            } catch (e: Exception) {
                source.sendMessage("Failed to fetch versions for $software: ${e.message}")
            }
        }
    }
}
