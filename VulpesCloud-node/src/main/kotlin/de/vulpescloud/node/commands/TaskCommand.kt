package de.vulpescloud.node.commands

import de.vulpescloud.api.lang.Translator
import de.vulpescloud.api.mysql.TaskTable
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.service.ServiceFactory
import de.vulpescloud.node.service.ServiceProviderImpl
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.setups.TaskSetup
import de.vulpescloud.node.terminal.JLineTerminal
import java.util.stream.Stream
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

@Suppress("Unused")
@Description("COMMANDS.DESCRIPTION.task")
class TaskCommand(
    private val setupProvider: SetupProvider,
    private val taskProvider: TaskProvider,
    private val translator: Translator,
    private val terminal: JLineTerminal,
    private val config: NodeConfig,
    private val versionProvider: VersionProvider,
    private val serviceFactory: ServiceFactory,
    serviceProvider: ServiceProvider,
) {

    private val serviceProvider = serviceProvider as ServiceProviderImpl
    private val logger = LoggerFactory.getLogger(javaClass)

    @Suggestions("tasks")
    fun suggestTasks(): Stream<String> {
        return taskProvider.tasks().stream().map { it.name }
    }

    @Parser(suggestions = "tasks")
    fun taskParser(input: CommandInput): Task {
        val command = input.readString()
        val task =
            taskProvider.tasks().find { it.name.equals(command, true) }
                ?: throw IllegalArgumentException("Task does not exist!")

        return task
    }

    @Command("task|tasks setup|create")
    fun startSetup() {
        setupProvider.startSetup(
            TaskSetup(taskProvider, translator, terminal, versionProvider, config)
        )
    }

    @Command("task|tasks list")
    fun listTasks(source: CommandSource) {
        val tasks = taskProvider.tasks()
        val maxNameLength = tasks.maxOfOrNull { it.name.length } ?: 0
        source.sendMessage("The following ${tasks.size} task(s) are registered:")
        tasks.forEach {
            val paddedName = it.name.padEnd(maxNameLength)
            source.sendMessage(
                " &8- &m$paddedName &7Maintenance: &e${it.maintenance}&8, &7MaxPlayers: &e${it.maxPlayers}&8, &7MaxMemory: &e${it.maxMemory}MB&8, &7Static: &e${it.staticServices}&8, &7Fallback: &e${it.fallback}&8, &7StartPort: &e${it.startPort}&8, &7Version: &e${it.version.name}-${it.version.version}"
            )
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
            serviceProvider.localServices
                .filter { it.task.name == task.name }
                .forEach {
                    source.sendMessage("Force stopping all services for task &m${task.name}")
                    it.forceStop()
                }
        } else {
            source.sendMessage("Stopping all services for task &m${task.name}")
            source.sendMessage("Not yet implemented")
            // task.services.forEach { it.stop() }
        }
    }

    @Confirmation
    @Command("task|tasks task <task> delete")
    fun deleteTask(source: CommandSource, @Argument("task") task: Task) {
        source.sendMessage("Deleting task &m${task.name}")
        stopAllServicesOnTask(source, task, true)
        transaction { TaskTable.deleteWhere { name eq task.name } }
        getRC()?.deleteHashField("VULPESCLOUD_TASKS", task.name)
    }

    @Command("task|tasks task <task> set maxMemory <memory>")
    fun setMaxMemory(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("memory") memory: Int,
    ) {
        source.sendMessage("Setting max memory for task &m${task.name} to &e$memory MB")
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                memory,
                task.maxPlayers,
                task.staticServices,
                task.minOnlineCount,
                task.serviceCount,
                task.services,
                task.maintenance,
                task.startPort,
                task.fallback,
                task.version,
                task.copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> set maxPlayers <players>")
    fun setMaxPlayers(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("players") players: Int,
    ) {
        source.sendMessage("Setting max players for task &m${task.name} to &e$players")
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                task.maxMemory,
                players,
                task.staticServices,
                task.minOnlineCount,
                task.serviceCount,
                task.services,
                task.maintenance,
                task.startPort,
                task.fallback,
                task.version,
                task.copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> set staticServices <static>")
    fun setStaticServices(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("static") static: Boolean,
    ) {
        source.sendMessage("Setting static services for task &m${task.name} to &e$static")
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                task.maxMemory,
                task.maxPlayers,
                static,
                task.minOnlineCount,
                task.serviceCount,
                task.services,
                task.maintenance,
                task.startPort,
                task.fallback,
                task.version,
                task.copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> set minOnlineCount <count>")
    fun setMinOnlineCount(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("count") count: Int,
    ) {
        source.sendMessage("Setting min online count for task &m${task.name} to &e$count")
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                task.maxMemory,
                task.maxPlayers,
                task.staticServices,
                count,
                task.serviceCount,
                task.services,
                task.maintenance,
                task.startPort,
                task.fallback,
                task.version,
                task.copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> set maintenance <maintenance>")
    fun setMaintenance(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("maintenance") maintenance: Boolean,
    ) {
        source.sendMessage("Setting maintenance for task &m${task.name} to &e$maintenance")
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                task.maxMemory,
                task.maxPlayers,
                task.staticServices,
                task.minOnlineCount,
                task.serviceCount,
                task.services,
                maintenance,
                task.startPort,
                task.fallback,
                task.version,
                task.copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> set startPort <port>")
    fun setStartPort(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("port") port: Int,
    ) {
        source.sendMessage("Setting start port for task &m${task.name} to &e$port")
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                task.maxMemory,
                task.maxPlayers,
                task.staticServices,
                task.minOnlineCount,
                task.serviceCount,
                task.services,
                task.maintenance,
                port,
                task.fallback,
                task.version,
                task.copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> set fallback <fallback>")
    fun setFallback(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("fallback") fallback: Boolean,
    ) {
        source.sendMessage("Setting fallback for task &m${task.name} to &e$fallback")
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                task.maxMemory,
                task.maxPlayers,
                task.staticServices,
                task.minOnlineCount,
                task.serviceCount,
                task.services,
                task.maintenance,
                task.startPort,
                fallback,
                task.version,
                task.copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> set copyTemplateToStatic <copyTemplateToStatic>")
    fun setCopyTemplateToStatic(
        source: CommandSource,
        @Argument("task") task: Task,
        @Argument("copyTemplateToStatic") copyTemplateToStatic: Boolean,
    ) {
        source.sendMessage(
            "Setting copy template to static for task &m${task.name} to &e$copyTemplateToStatic"
        )
        val newTask =
            Task(
                task.name,
                task.nodes,
                task.templates,
                task.maxMemory,
                task.maxPlayers,
                task.staticServices,
                task.minOnlineCount,
                task.serviceCount,
                task.services,
                task.maintenance,
                task.startPort,
                task.fallback,
                task.version,
                copyTemplateToStatic,
            )
        taskProvider.updateTask(newTask)
    }

    @Command("task|tasks task <task> info")
    fun taskInfo(source: CommandSource, @Argument("task") task: Task) {
        source.sendMessage("Task &m${task.name} &7Info:")
        source.sendMessage(" &8- &7Max Memory: &e${task.maxMemory}MB")
        source.sendMessage(" &8- &7Max Players: &e${task.maxPlayers}")
        source.sendMessage(" &8- &7Static Services: &e${task.staticServices}")
        source.sendMessage(" &8- &7Min Online Count: &e${task.minOnlineCount}")
        source.sendMessage(" &8- &7Service Count: &e${task.serviceCount}")
        source.sendMessage(" &8- &7Maintenance: &e${task.maintenance}")
        source.sendMessage(" &8- &7Start Port: &e${task.startPort}")
        source.sendMessage(" &8- &7Fallback: &e${task.fallback}")
        source.sendMessage(" &8- &7Version: &e${task.version}")
        source.sendMessage(" &8- &7Templates: &e${task.templates.joinToString(", ") { it.name }}")
        source.sendMessage(" &8- &7Services: &e${task.services.joinToString(", ") { it.name }}")
        source.sendMessage(" &8- &7Nodes: &e${task.nodes.joinToString(", ")}")
    }
}
