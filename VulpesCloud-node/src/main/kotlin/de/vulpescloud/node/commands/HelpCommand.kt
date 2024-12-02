package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.util.stream.Stream

@Suppress("UNUSED")
@Description("See all Commands!(translatable please!)", false)
@Alias(["?"])
class HelpCommand {

    @Parser(suggestions = "commands")
    fun commandInfoParser(input: CommandInput): CommandInfo {
        val command = input.readString()
        val commandInfo = Node.instance.commandProvider.command(command) ?: throw IllegalStateException()

        return commandInfo
    }

    @Suggestions("commands")
    fun suggestCommands(): Stream<String> {
        return Node.instance.commandProvider.commands()!!.stream().map { it.name }
    }

    @Command("help|?")
    fun sendGeneralHelp(source: CommandSource) {
        Node.instance.commandProvider.commands()!!.forEach {
            source.sendMessage("&f${it.name}${it.aliases} &8- &7${it.description}&8.")
        }
    }

    @Command("help|? <command>")
    fun sendSpecificHelp(
        source: CommandSource,
        @Argument("command") command: CommandInfo?) {
        if (command != null) {
            source.sendMessage("Aliases: &m${command.joinNameToAliases(", ")}")
            source.sendMessage("Description: &m${command.description}")
            source.sendMessage("Usages:")
            command.usage.forEach { source.sendMessage(" > &m$it") }
        } else {
            source.sendMessage("&cInvalid command!")
        }
    }

}