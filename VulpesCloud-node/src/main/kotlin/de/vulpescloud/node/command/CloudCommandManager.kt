package de.vulpescloud.node.command

import de.vulpescloud.node.command.source.CommandSource
import lombok.NonNull
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer


class CloudCommandManager(
    executionCoordinator: ExecutionCoordinator<CommandSource>,
) : CommandManager<CommandSource>(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler()) {

    companion object {
        lateinit var instance: CloudCommandManager
    }

    val logger: Logger = LoggerFactory.getLogger(CloudCommandManager::class.java)

    fun bootstrap(
        @NonNull args: @NonNull Array<String?>,
        @NonNull managerConsumer: Consumer<CommandManager<CommandSource>?>,
    ) {
        val cliCommandManager: CommandManager<CommandSource> =
            CloudCommandManager(ExecutionCoordinator.simpleCoordinator())
        managerConsumer.accept(cliCommandManager)
        instance.run(args)
    }

//    private var permissionFunction: PermissionFunction = PermissionFunction.alwaysTrue()

    init {
        instance = this

        this.registerDefaultExceptionHandlers(
            { triplet ->
                val message = triplet.first().formatCaption(triplet.second(), triplet.third())
                logger.error(message)
            },
            { pair ->
                logger.error(pair.first())
                pair.second().printStackTrace()
            }
        )
    }

    override fun hasPermission(sender: CommandSource, permission: String): Boolean {
        return true
    }

    fun run(args: Array<String?>) {
        Objects.requireNonNull(args, "args")
        try {
            commandExecutor().executeCommand(CommandSource.console(), java.lang.String.join(" ", *args)).join()
        } catch (e: Exception) {
            logger.error(e.toString())
        }
    }

//    fun permissionFunction(permissionFunction: PermissionFunction) {
//        this.permissionFunction = Objects.requireNonNull(permissionFunction)
//    }


}