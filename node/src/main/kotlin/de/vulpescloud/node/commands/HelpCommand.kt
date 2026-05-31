package de.vulpescloud.node.commands

import de.vulpescloud.node.command.CommandInfo
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.util.stream.Stream

@Suppress("UNUSED")
@Alias(["?"])
class HelpCommand(private val commandProvider: CommandProvider) {

    @Parser(suggestions = "commands")
    fun commandInfoParser(input: CommandInput): CommandInfo {
        val command = input.readString()
        val commandInfo = commandProvider.command(command) ?: throw IllegalStateException()

        return commandInfo
    }

    @Suggestions("commands")
    fun suggestCommands(): Stream<String> {
        return commandProvider.commands()!!.stream().map { it.name }
    }

    @Command("help|?")
    fun sendGeneralHelp(source: CommandSource) {
        commandProvider.commands()!!.forEach {
            source.sendMessage(" <dark_gray>»</dark_gray> <white>${it.joinNameToAliases(", ")}</white> <dark_gray>-</dark_gray> <gray>${it.description}</gray>")
        }
    }

    @Command("help|? <command>")
    fun sendSpecificHelp(source: CommandSource, @Argument("command") command: CommandInfo?) {
        if (command != null) {
            source.sendMessage("<gray>Aliases<dark_gray>:</dark_gray> <white>${command.joinNameToAliases(", ")}</white>")
            source.sendMessage("<gray>Description<dark_gray>:</dark_gray> <white>${command.description}</white>")
            source.sendMessage("<gray>Usages<dark_gray>:</dark_gray>")
            command.usage.forEach { source.sendMessage(" <dark_gray>»</dark_gray> <white>$it</white>") }
        } else {
            source.sendMessage("<red>Invalid command!</red>")
        }
    }
}