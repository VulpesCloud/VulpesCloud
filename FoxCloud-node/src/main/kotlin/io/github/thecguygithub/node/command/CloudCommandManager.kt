package io.github.thecguygithub.node.command

import io.github.thecguygithub.node.command.source.CommandSource
import io.github.thecguygithub.node.logging.Logger
import lombok.NonNull
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler
import java.util.*
import java.util.function.Consumer


class CloudCommandManager(
    executionCoordinator: ExecutionCoordinator<CommandSource>,
) : CommandManager<CommandSource>(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler()) {

    companion object {
        lateinit var instance: CloudCommandManager
    }

    fun bootstrap(
        @NonNull args: @NonNull Array<String?>,
        @NonNull managerConsumer: Consumer<CommandManager<CommandSource>?>,
    ) {
        val cliCommandManager: CommandManager<CommandSource> = CloudCommandManager(ExecutionCoordinator.simpleCoordinator())
        managerConsumer.accept(cliCommandManager)
        instance.run(args)
    }

//    private var permissionFunction: PermissionFunction = PermissionFunction.alwaysTrue()

    init {
        instance = this

        this.registerDefaultExceptionHandlers(
            { triplet ->
                val message = triplet.first().formatCaption(triplet.second(), triplet.third())
                Logger().error(message)
            },
            { pair ->
                Logger().error(pair.first())
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
            Logger().error(e.toString())
        }
    }

//    fun permissionFunction(permissionFunction: PermissionFunction) {
//        this.permissionFunction = Objects.requireNonNull(permissionFunction)
//    }
    

}