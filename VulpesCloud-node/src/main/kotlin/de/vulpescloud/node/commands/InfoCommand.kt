package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import de.vulpescloud.node.logging.Logger
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister

class InfoCommand {
    init {

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
                Logger().info("Java version&8: &f${System.getProperty("java.version")}")
                Logger().info("Operating System&8: &f${System.getProperty("os.name")}")
                Logger().info("Used Memory of the Node process&8: &f${usedMemory()}")
                Logger().info(" ")
            }
        }
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
            .freeMemory()) / 1024 / 1024).toString() + " mb"
    }

}