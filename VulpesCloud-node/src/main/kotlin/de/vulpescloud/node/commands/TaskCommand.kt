package de.vulpescloud.node.commands

import de.vulpescloud.api.lang.Translator
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.service.ServiceFactory
import de.vulpescloud.node.service.ServiceProviderImpl
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.setups.TaskSetup
import de.vulpescloud.node.terminal.JLineTerminal
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.slf4j.LoggerFactory
import java.util.stream.Stream

@Suppress("Unused")
class TaskCommand(
    val setupProvider: SetupProvider,
    val taskProvider: TaskProvider,
    val translator: Translator,
    val terminal: JLineTerminal,
    val config: NodeConfig,
    val versionProvider: VersionProvider,
    val serviceFactory: ServiceFactory,
    serviceProvider: ServiceProvider
) {

    private val serviceProvider = serviceProvider as ServiceProviderImpl
    private val logger = LoggerFactory.getLogger(javaClass)

    @Suggestions("tasks")
    fun suggestTasks(): Stream<String> {
        try {
            logger.debug("Trying to suggest tasks")
            return taskProvider.tasks().stream().map { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf<Task>().stream().map { it.name }
        }
    }

    @Parser(suggestions = "tasks")
    fun taskParser(input: CommandInput): Task {
        val command = input.readString()
        val task = taskProvider.tasks().find { it.name.equals(command, true) }
            ?: throw IllegalArgumentException("Task does not exist!")

        return task
    }

    @Command("task|tasks create")
    fun startSetup() {
        setupProvider.startSetup(
            TaskSetup(taskProvider, translator, terminal, versionProvider, config)
        )
    }

    @Command("task|tasks list")
    fun listTasks(
        source: CommandSource
    ) {
        source.sendMessage("The following ${taskProvider.tasks().size} task(s) are registered:")
        taskProvider.tasks().forEach {
            source.sendMessage(" &8- &m${it.name} &7Version: &e${it.version} &7MaxPlayers: &e${it.maxPlayers} &7MaxMemory: &e${it.maxMemory}MB &7Static: &e${it.staticServices}")
        }
    }

    @Command("task|tasks task <task> prepare")
    fun prepareService(
        source: CommandSource,
        @Argument("task") task: Task,
        @Flag("start") startService: Boolean,
    ) {
        if (startService) {
            source.sendMessage("Preparing and starting service for task &m${task.name}")
            serviceFactory.prepareService(task).second.start()
        } else {
            source.sendMessage("Preparing service for task &m${task.name}")
            serviceFactory.prepareService(task).second
        }
    }

    @Command("task|tasks task <task> stop")
    fun stopAllServicesOnTask(
        source: CommandSource,
        @Argument("task") task: Task,
        @Flag("force") force: Boolean,
    ) {
        if (force) {
            serviceProvider.localServices.filter { it.task.name == task.name }.forEach {
                source.sendMessage("Force stopping all services for task &m${task.name}")
                it.forceStop()
            }
        } else {
            source.sendMessage("Stopping all services for task &m${task.name}")
            source.sendMessage("Not yet implemented")
            //task.services.forEach { it.stop() }
        }
    }
}
