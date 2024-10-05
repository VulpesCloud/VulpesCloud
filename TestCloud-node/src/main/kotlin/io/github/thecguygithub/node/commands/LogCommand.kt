package io.github.thecguygithub.node.commands

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.command.Command
import io.github.thecguygithub.node.command.CommandArgumentType
import io.github.thecguygithub.node.command.CommandArgumentType.Keyword
import io.github.thecguygithub.node.command.CommandContext
import io.github.thecguygithub.node.command.CommandExecution
import io.github.thecguygithub.node.config.LogLevels


class LogCommand : Command("log", "Change the Log Level of the Cloud") {
    init {
        val logLevel = CommandArgumentType.Text("LogLevel")
        syntax(object : CommandExecution {
            override fun execute(commandContext: CommandContext) {
                val ll = commandContext.arg(logLevel)
                Node.nodeConfig?.logLevel = ll as LogLevels?
            }
        },"Change the Log Level of the Cloud" , logLevel)
    }
}