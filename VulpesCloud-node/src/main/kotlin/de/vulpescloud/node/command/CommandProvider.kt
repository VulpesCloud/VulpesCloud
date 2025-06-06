package de.vulpescloud.node.command

import de.vulpescloud.node.command.impl.CommandInfo
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.CommandResult
import java.util.concurrent.CompletableFuture

interface CommandProvider {

    val commandManager: CommandManager<CommandSource>

    fun initialize()

    fun suggest(source: CommandSource, input: String): List<String>

    fun execute(source: CommandSource, input: String): CompletableFuture<CommandResult<CommandSource>>

    fun register(command: Any)

    fun command(name: String): CommandInfo?

    fun commands(): MutableCollection<CommandInfo>?

    fun commandUsageOfRoot(root: String): List<String>
}