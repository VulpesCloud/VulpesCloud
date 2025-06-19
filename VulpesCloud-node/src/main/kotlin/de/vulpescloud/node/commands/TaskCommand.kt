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
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.stream.Stream

@Suppress("Unused")
@Description("COMMANDS.DESCRIPTION.task")
class TaskCommand(
    private val setupProvider: SetupProvider,
    private val taskProvider: TaskProvider,
    private val translator: Translator,
    private val terminal: JLineTerminal,
    private val config: NodeConfig,
    private val versionProvider: VersionProvider,
    serviceProvider: ServiceProvider,
) {
    private val serviceProvider = serviceProvider as ServiceProviderImpl
    private val logger = LoggerFactory.getLogger(javaClass)

    @Suggestions("tasks")
    fun suggestTasks(): Stream<String> {
        return taskProvider.tasks().stream().map { it.name }
    }

    @Parser(suggestions = "tasks")
    fun taskParser(input: CommandInput): List<Task> {
        val regexPattern = input.readString()
        val regex = Regex(regexPattern)
        return taskProvider.tasks().filter { regex.matches(it.name) }
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

    @Command("task|tasks task <tasks> prepare")
    fun prepareService(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Flag("start") startService: Boolean,
    ) {
        tasks.forEach { task ->
            if (startService) {
                source.sendMessage("Preparing and starting service for task &m${task.name}")
                val factory =
                    serviceProvider.serviceFactories.find { it.name() == task.serviceFactoryName }
                if (factory == null) {
                    source.sendMessage(
                        "Could not start Service: ServiceFactory &m${task.serviceFactoryName}&7 was not found!"
                    )
                    return@forEach
                }
                factory.prepareService(task).start()
            } else {
                source.sendMessage("Preparing service for task &m${task.name}")
                val factory =
                    serviceProvider.serviceFactories.find { it.name() == task.serviceFactoryName }
                if (factory == null) {
                    source.sendMessage(
                        "Could not prepare Service: ServiceFactory &m${task.serviceFactoryName}&7 was not found!"
                    )
                    return@forEach
                }
                factory.prepareService(task)
            }
        }
    }

    @Command("task|tasks task <tasks> stop")
    fun stopAllServicesOnTask(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Flag("force") force: Boolean,
    ) {
        tasks.forEach { task ->
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
            }
        }
    }

    @Confirmation
    @Command("task|tasks task <tasks> delete")
    fun deleteTask(source: CommandSource, @Argument("tasks") tasks: List<Task>) {
        tasks.forEach { task ->
            source.sendMessage("Deleting task &m${task.name}")
            stopAllServicesOnTask(source, listOf(task), true)
            transaction { TaskTable.deleteWhere { name eq task.name } }
            getRC()?.deleteHashField("VULPESCLOUD_TASKS", task.name)
        }
    }

    @Command("task|tasks task <tasks> set maxMemory <memory>")
    fun setMaxMemory(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("memory") memory: Int,
    ) {
        tasks.forEach { task ->
            source.sendMessage("Setting max memory for task &m${task.name} to &e$memory MB")
            val newTask = task.copy(maxMemory = memory)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> set maxPlayers <players>")
    fun setMaxPlayers(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("players") players: Int,
    ) {
        tasks.forEach { task ->
            source.sendMessage("Setting max players for task &m${task.name} to &e$players")
            val newTask = task.copy(maxPlayers = players)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> set staticServices <static>")
    fun setStaticServices(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("static") static: Boolean,
    ) {
        tasks.forEach { task ->
            source.sendMessage("Setting static services for task &m${task.name} to &e$static")
            val newTask = task.copy(staticServices = static)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> set minOnlineCount <count>")
    fun setMinOnlineCount(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("count") count: Int,
    ) {
        tasks.forEach { task ->
            source.sendMessage("Setting min online count for task &m${task.name} to &e$count")
            val newTask = task.copy(minOnlineCount = count)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> set maintenance <maintenance>")
    fun setMaintenance(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("maintenance") maintenance: Boolean,
    ) {
        tasks.forEach { task ->
            source.sendMessage("Setting maintenance for task &m${task.name} to &e$maintenance")
            val newTask = task.copy(maintenance = maintenance)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> set startPort <port>")
    fun setStartPort(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("port") port: Int,
    ) {
        tasks.forEach { task ->
            source.sendMessage("Setting start port for task &m${task.name} to &e$port")
            val newTask = task.copy(startPort = port)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> set fallback <fallback>")
    fun setFallback(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("fallback") fallback: Boolean,
    ) {
        tasks.forEach { task ->
            source.sendMessage("Setting fallback for task &m${task.name} to &e$fallback")
            val newTask = task.copy(fallback = fallback)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> set copyTemplateToStatic <copyTemplateToStatic>")
    fun setCopyTemplateToStatic(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("copyTemplateToStatic") copyTemplateToStatic: Boolean,
    ) {
        tasks.forEach { task ->
            source.sendMessage(
                "Setting copy template to static for task &m${task.name} to &e$copyTemplateToStatic"
            )
            val newTask = task.copy(copyTemplateToStatic = copyTemplateToStatic)
            taskProvider.updateTask(newTask)
        }
    }

    @Command("task|tasks task <tasks> info")
    fun taskInfo(source: CommandSource, @Argument("tasks") tasks: List<Task>) {
        tasks.forEach { task ->
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
            source.sendMessage(
                " &8- &7Templates: &e${task.templates.joinToString(", ") { it.name }}"
            )
            source.sendMessage(" &8- &7Services: &e${task.services.joinToString(", ") { it.name }}")
            source.sendMessage(" &8- &7Nodes: &e${task.nodes.joinToString(", ")}")
            source.sendMessage(" &8- &7Copy Template To Static: &e${task.copyTemplateToStatic}")
            source.sendMessage(" &8- &7Service Factory: &e${task.serviceFactoryName}")
            source.sendMessage(
                " &8- &7Environment Variables: &e${task.environmentVars.joinToString(", ") { it.first + "=" + it.second }}"
            )
            source.sendMessage("------------------------")
        }
    }
}
