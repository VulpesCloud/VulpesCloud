package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.command.annotation.Alias
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.processors.confirmation.annotation.Confirmation

@Alias(["exit", "quit", "bye"])
class ExitCommand {

    @Command("stop|exit|quit|bye")
    @Confirmation
    fun exit() {
        NodeShutdown.shutdown()
    }

}
