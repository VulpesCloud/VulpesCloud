package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
@Description("COMMANDS.DESCRIPTION.exit")
@Alias(["stop", "shutdown"])
class ExitCommand {

    @Command("exit|stop|shutdown")
    fun shutdown(
        source: CommandSource
    ) {
        source.sendMessage("Shutting down the Node!")
        NodeShutdown.commandShutdown()
    }

}