package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.command.CommandInfo
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.NodeShutdown
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister


class ShutdownCommand {
//    init {
//
//        Node.commandProvider?.registeredCommands?.add(
//            CommandInfo(
//                "shutdown",
//                setOf("stop", "exit"),
//                "Shutdown the cloud and all node services.",
//                listOf("clear")
//            )
//        )
//
//        Node.commandProvider?.commandManager!!.buildAndRegister(
//            "shutdown", Description.of("Shutdown the cloud and all node services."), aliases = arrayOf("exit", "stop")) {
//            handler { _ ->
//                NodeShutdown.nodeShutdown(false)
//            }
//        }
//    }
}