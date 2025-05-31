package de.vulpescloud.node.commands

import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.terminal.JLineTerminal
import org.incendo.cloud.annotations.Command

@Description("COMMANDS.DESCRIPTION.clear")
class ClearCommand(private val terminal: JLineTerminal) {

    @Command("clear|cls")
    fun clear() {
        terminal.clear()
    }
}
