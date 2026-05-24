package de.vulpescloud.node.commands

import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import de.vulpescloud.node.command.annotation.Alias
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.processors.confirmation.annotation.Confirmation

@Alias(["exit", "quit", "bye"])
class ExitCommand {

    @OptIn(DelicateCoroutinesApi::class)
    @Command("stop|exit|quit|bye")
    @Confirmation
    fun exit(source: CommandSource) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        GlobalScope.launch { NodeShutdown.shutdown() }
    }
}
