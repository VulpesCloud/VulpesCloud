package io.github.thecguygithub.node.commands

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.command.Command
import io.github.thecguygithub.node.command.CommandContext
import io.github.thecguygithub.node.command.CommandExecution


class ClearCommand : Command("clear", "Clears the Terminal") {
    init {
        defaultExecution(object : CommandExecution {
            override fun execute(commandContext: CommandContext) {
                Node.terminal!!.clear()
            }
        })
    }
}