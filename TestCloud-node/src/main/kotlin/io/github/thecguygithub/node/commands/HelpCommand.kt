package io.github.thecguygithub.node.commands

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.command.Command
import io.github.thecguygithub.node.command.CommandContext
import io.github.thecguygithub.node.command.CommandExecution
import io.github.thecguygithub.node.logging.Logger

class HelpCommand : Command("help", "Shows you all available Commands", "?") {
    init {
        defaultExecution(object : CommandExecution {
            override fun execute(commandContext: CommandContext) {
                for (command in Node.commandProvider!!.commands()) {
                    val aliases = if (command!!.aliases.isNotEmpty()) {
                        " &8(&7" + command.aliases.joinToString("&8, &7") + "&8)"
                    } else {
                        ""
                    }
                    Logger().info("&f${command.name}${aliases} &8- &7${command.description}&8.")
                }
            }
        })
    }
}