package de.vulpescloud.node.commands

import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.terminal.Terminal
import org.incendo.cloud.annotations.Command

@Alias(["cls"])
class ClearCommand(private val terminal: Terminal) {

    @Command("clear|cls")
    fun clear() {
        terminal.clear()
    }

}
