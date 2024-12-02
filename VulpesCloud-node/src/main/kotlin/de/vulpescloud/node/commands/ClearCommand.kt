package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
@Description("Clear the Screen(translate)")
@Alias(["cls"])
class ClearCommand {

    @Command("clear|cls")
    fun clear() {
        Node.instance.terminal.clear()
    }

}