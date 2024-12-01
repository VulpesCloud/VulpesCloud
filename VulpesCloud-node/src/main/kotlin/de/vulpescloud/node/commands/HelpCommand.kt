package de.vulpescloud.node.commands

import de.vulpescloud.api.Named
import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import java.util.stream.Stream

@Description("See all Commands!(translatalbe please!)", false)
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
    fun sendHelp(source: CommandSource) {
        Node.instance.commandProvider.commands()!!.forEach {
            source.sendMessage("&f${it.name}${it.aliases} &8- &7${it.description}&8.")
        }
    }

}