package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.command.source.CommandSource
import de.vulpescloud.node.version.Version
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.util.stream.Stream

@Suppress("UNUSED")
@Description("Manage all Versions (translate)")
@Alias(["versions", "ver"])
class VersionCommand {
    private val versionProvider = Node.instance.versionProvider

    @Parser(suggestions = "versions")
    fun commandInfoParser(input: CommandInput): Version {
        val command = input.readString()
        val version = Node.instance.versionProvider.getByName(command) ?: throw IllegalStateException()
        return version
    }

    @Suggestions("versions")
    fun suggestCommands(): Stream<String> {
        return Node.instance.versionProvider.versions.stream().map { it.environment.name }
    }

    @Command("version|versions|ver list")
    fun listVersions(
        source: CommandSource
    ) {
        source.sendMessage("The Following ${versionProvider.versions.size} Versions are loaded:")
        versionProvider.versions.forEach { source.sendMessage(" - ${it.environment.name} (${it.type.name})") }
    }

    @Command("version|versions|ver <version>")
    fun listSingleVersion(
        source: CommandSource,
        @Argument("version") version: Version
    ) {
        source.sendMessage("The following ${version.environment.name} Versions are loaded:")
        version.versions.forEach { source.sendMessage(" - ${it.version}") }
    }

}