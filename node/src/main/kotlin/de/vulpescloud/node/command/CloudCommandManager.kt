package de.vulpescloud.node.command

import de.vulpescloud.node.grpc.security.PermissionHelper
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler

class CloudCommandManager :
    CommandManager<CommandSource>(
        ExecutionCoordinator.asyncCoordinator(),
        CommandRegistrationHandler.nullCommandRegistrationHandler(),
    ) {

    override fun hasPermission(sender: CommandSource, permission: String): Boolean {
        return runBlocking {
            return@runBlocking when (sender) {
                is ConsoleCommandSource -> true
                is InternalPlayerCommandSource ->
                    PermissionHelper.hasPermission(sender.user.name, permission)
                else -> throw UnsupportedOperationException()
            }
        }
    }
}
