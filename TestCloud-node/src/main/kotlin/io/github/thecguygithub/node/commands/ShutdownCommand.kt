package io.github.thecguygithub.node.commands

import io.github.thecguygithub.node.NodeShutdown
import io.github.thecguygithub.node.command.Command
import io.github.thecguygithub.node.command.CommandContext
import io.github.thecguygithub.node.command.CommandExecution


class ShutdownCommand : Command("shutdown", "Shutdown the cloud and all node services", "stop", "exit") {
    init {
        defaultExecution(object : CommandExecution {
            override fun execute(commandContext: CommandContext) {
                NodeShutdown.nodeShutdown(false)
            }
        })
    }
}