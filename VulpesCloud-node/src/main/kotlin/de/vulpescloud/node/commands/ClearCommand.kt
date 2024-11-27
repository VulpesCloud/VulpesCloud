package de.vulpescloud.node.commands

import de.vulpescloud.api.command.CommandInfo
import de.vulpescloud.node.Node
import org.incendo.cloud.kotlin.extension.buildAndRegister

class ClearCommand {
    init {

        Node.commandProvider?.registeredCommands?.add(
            CommandInfo(
                "clear",
                setOf("cls"),
                "Clear the Terminal.",
                listOf("clear")
            )
        )

        Node.commandProvider?.commandManager!!.buildAndRegister(
            "clear", aliases = arrayOf("cls")
        ) {
            handler { _ ->
                Node.terminal!!.clear()
            }
        }
    }
}