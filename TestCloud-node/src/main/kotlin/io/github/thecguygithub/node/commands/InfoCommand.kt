package io.github.thecguygithub.node.commands

import io.github.thecguygithub.node.command.Command
import io.github.thecguygithub.node.command.CommandContext
import io.github.thecguygithub.node.command.CommandExecution
import io.github.thecguygithub.node.logging.Logger

class InfoCommand : Command("info", "Gives information about the Node", "me") {
    init {
        defaultExecution(object : CommandExecution {
            override fun execute(commandContext: CommandContext) {
                Logger().info("Java version&8: &f${System.getProperty("java.version")}");
                Logger().info("Operating System&8: &f${System.getProperty("os.name")}");
                Logger().info("Used Memory of the Node process&8: &f${usedMemory()}");
                Logger().info(" ");
            }
        })
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
            .freeMemory()) / 1024 / 1024).toString() + " mb"
    }

}