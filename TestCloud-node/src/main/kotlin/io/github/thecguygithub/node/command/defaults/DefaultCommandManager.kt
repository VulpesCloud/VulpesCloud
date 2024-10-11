package io.github.thecguygithub.node.command.defaults

import io.github.thecguygithub.node.command.source.CommandSource
import lombok.NonNull
import org.incendo.cloud.CloudCapability
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.meta.SimpleCommandMeta
import java.util.concurrent.Executors


class DefaultCommandManager<T> internal constructor() : CommandManager<CommandSource>(
    ExecutionCoordinator.builder<CommandSource>().executor(Executors.newFixedThreadPool(4)).build(),
    CommandRegistrationHandler.nullCommandRegistrationHandler<CommandSource>()
) {
    /**
     * Constructs the default implementation of the [CommandManager]. Applying asynchronous command executing using
     * a thread pool with 4 threads.
     */
    init {
        this.registerCapability(CloudCapability.StandardCapabilities.ROOT_COMMAND_DELETION)
    }

    /**
     * {@inheritDoc}
     */
    override fun hasPermission(@NonNull sender: CommandSource, @NonNull permission: String): Boolean {
        return sender.checkPermission(permission)
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    override fun createDefaultCommandMeta(): CommandMeta {
        return SimpleCommandMeta.empty()
    }
}