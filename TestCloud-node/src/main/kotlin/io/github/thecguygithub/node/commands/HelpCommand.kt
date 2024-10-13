package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister

class HelpCommand {
    init {
        Node.commandProvider?.registeredCommands?.add(CommandInfo("help", setOf("?"), "Get information about all Commands!", listOf("help | ?")))

        val commandInfo = Node.commandProvider?.registeredCommands

        Node.commandProvider?.commandManager!!.buildAndRegister(
            "help", Description.of("Get information about all Commands!"), aliases = arrayOf("?")) {
            handler { _ ->
                    Logger().info("All registered Commands:")
                commandInfo!!.forEach { commandInfo ->
                    Logger().info("&f${commandInfo.name}${commandInfo.aliases} &8- &7${commandInfo.description}&8.")
                }
            }
        }
    }
}