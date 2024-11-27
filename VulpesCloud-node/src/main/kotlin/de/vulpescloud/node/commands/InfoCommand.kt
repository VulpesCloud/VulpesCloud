package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InfoCommand {
    init {
        val logger: Logger = LoggerFactory.getLogger(InfoCommand::class.java)
        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "info",
                setOf("me"),
                "Gives information about the Node.",
                listOf("info | me")
            )
        )

        Node.commandProvider?.commandManager!!.buildAndRegister(
            "info", Description.of("Gives information about the Node."), aliases = arrayOf("me")) {
            handler { _ ->
                logger.info("Java version&8: &f${System.getProperty("java.version")}")
                logger.info("Operating System&8: &f${System.getProperty("os.name")}")
                logger.info("Used Memory of the Node process&8: &f${usedMemory()}")
                logger.info(" ")
            }
        }
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
            .freeMemory()) / 1024 / 1024).toString() + " mb"
    }

}