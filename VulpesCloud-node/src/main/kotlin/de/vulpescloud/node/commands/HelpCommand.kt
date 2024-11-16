package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.slf4j.LoggerFactory

class HelpCommand {
    init {
        val logger = LoggerFactory.getLogger(HelpCommand::class.java)
        Node.commandProvider?.registeredCommands?.add(CommandInfo("help", setOf("?"), "Get information about all Commands!", listOf("help | ?")))

        val commandInfo = Node.commandProvider?.registeredCommands

        Node.commandProvider?.commandManager!!.buildAndRegister(
            "help", Description.of("Get information about all Commands!"), aliases = arrayOf("?")) {
            handler { _ ->
                    logger.info("All registered Commands:")
                commandInfo!!.forEach { commandInfo ->
                    logger.info("&f${commandInfo.name}${commandInfo.aliases} &8- &7${commandInfo.description}&8.")
                }
            }
        }
    }
}