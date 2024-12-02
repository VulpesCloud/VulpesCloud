package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
@Description("Shut down the Node(translate)")
@Alias(["stop", "shutdown"])
class ExitCommand {

    @Command("exit|stop|shutdown")
    fun shutdown(
        source: CommandSource
    ) {
        source.sendMessage("Shutting down the Node!")
        NodeShutdown.normalShutdown()
    }

}