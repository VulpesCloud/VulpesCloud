package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.command.annotations.Description
import jakarta.inject.Singleton
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.command

@Singleton
@Permission("testcloud.command-clear")
@Description("Clears the Terminal")
class ClearCommand {

    @Command("clear")
    fun clearConsole() {
        Node.terminal!!.clear()
    }


//    init {
//
//        Node.commandProvider?.registeredCommands?.add(
//            CommandInfo(
//                "clear",
//                setOf("cls"),
//                "Clear the Terminal.",
//                listOf("clear")
//            )
//        )
//
//        Node.commandProvider?.commandManager!!.command(
//            Node.commandProvider?.commandManager!!.buildAndRegister(
//                "clear", aliases = arrayOf("cls")
//            ) {
//                handler { _ ->
//                    Node.terminal!!.clear()
//                }
//            }
//        )
//    }
}