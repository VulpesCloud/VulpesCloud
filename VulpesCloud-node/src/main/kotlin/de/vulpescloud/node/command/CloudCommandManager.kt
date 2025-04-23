package de.vulpescloud.node.command

import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler

class CloudCommandManager : CommandManager<CommandSource>(
    ExecutionCoordinator.simpleCoordinator(),
    CommandRegistrationHandler.nullCommandRegistrationHandler()
) {
    override fun hasPermission(sender: CommandSource, permission: String): Boolean = sender.checkPermission(permission)
}