package de.vulpescloud.node.commands

import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.terminal.Terminal
import org.incendo.cloud.annotations.Command

@Alias(["cls"])
class ClearCommand(private val terminal: Terminal) {

    @Command("clear|cls")
    fun clear(source: CommandSource) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        terminal.clear()
    }

}
