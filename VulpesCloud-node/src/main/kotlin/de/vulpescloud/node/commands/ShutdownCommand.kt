package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeShutdown
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister


class ShutdownCommand {
    init {

        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "shutdown",
                setOf("stop", "exit"),
                "Shutdown the cloud and all node services.",
                listOf("clear")
            )
        )

        Node.commandProvider?.commandManager!!.buildAndRegister(
            "exit", Description.of("Shutdown the cloud and all node services."), aliases = arrayOf("shutdown", "stop")) {
            handler { _ ->
                NodeShutdown.nodeShutdown(false)
            }
        }
    }
}