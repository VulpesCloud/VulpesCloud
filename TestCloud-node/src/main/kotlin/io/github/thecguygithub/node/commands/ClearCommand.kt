package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.command

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