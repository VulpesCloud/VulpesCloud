package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.command.annotation.Alias
import org.incendo.cloud.annotations.Command

@Alias(["exit", "quit", "bye"])
class ExitCommand {

    @Command("stop|exit|quit|bye")
    fun exit() {
        NodeShutdown.shutdown()
    }

}
