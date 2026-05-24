package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.tasks.v1.PrepareServiceOnTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.TaskDefinition
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.deleteTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import build.buf.gen.vulpescloud.tasks.v1.updateTaskRequest
import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.grpc.security.AuthClientInterceptor
import de.vulpescloud.node.setup.setups.TaskSetup
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation

@Suppress("UNUSED")
@Alias(["task"])
class TaskCommand {

    @Suggestions("tasks")
    fun taskSuggestions(): Stream<String> {
        return TaskCache.getTasks().map { it.name }.stream()
    }

    @Parser(suggestions = "tasks")
    fun taskParser(input: CommandInput): List<Task> {
        val raw = input.readString()
        val pattern = raw.split("*").joinToString(".*") { Regex.escape(it) }
        val regex = Regex("^$pattern$", RegexOption.IGNORE_CASE)

        return TaskCache.getTasks()
            .filter { regex.matches(it.name) }
            .map { Task.fromDefinition(it) }
    }

    @Command("task|tasks setup")
    fun setupTask(source: CommandSource) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        Node.instance.setupProvider.startSetup(TaskSetup())
    }

    @Permission("tasks.getAll")
    @Command("task|tasks list")
    fun listTasks(source: CommandSource) {
        runBlocking {
            val tasks =
                Node.instance.localGrpcClient.tasksAPI
                    .getAllTasks(getAllTasksRequest {})
                    .tasksList
                    .map { Task.fromDefinition(it) }

            val maxNameLength = tasks.maxOfOrNull { it.name.length } ?: 0

            source.sendMessage("The following ${tasks.size} task(s) are registered:")
            tasks.forEach {
                val name = it.name.padEnd(maxNameLength)
                source.sendMessage(
                    " <dark_gray>- <white>$name <gray>Maintenance: <yellow>${it.maintenance}<dark_gray>, <gray>MaxPlayers: <yellow>${it.maxPlayers}<dark_gray>, <gray>MaxMemory: <yellow>${it.maxMemory}MB<dark_gray>, <gray>Static: <yellow>${it.staticServices}<dark_gray>, <gray>Fallback: <yellow>${it.fallback}<dark_gray>, <gray>StartPort: <yellow>${it.startPort}<dark_gray>, <gray>Version: <yellow>${it.software.name}-${it.software.version}"
                )
            }
        }
    }

    @Permission("tasks.get")
    @Command("task|tasks task <tasks> info")
    fun infoTask(source: CommandSource, @Argument("tasks") tasks: List<Task>) {
        tasks.forEach {
            source.sendMessage(
                "<gray>Name: <yellow>${it.name} \n" +
                    "<gray>MaxPlayers: <yellow>${it.maxPlayers} \n" +
                    "<gray>MaxMemory: <yellow>${it.maxMemory}MB \n" +
                    "<gray>Static: <yellow>${it.staticServices} \n" +
                    "<gray>Fallback: <yellow>${it.fallback} \n" +
                    "<gray>StartPort: <yellow>${it.startPort} \n" +
                    "<gray>Version: <yellow>${it.software.name}-${it.software.version} \n" +
                    "<gray>StaticServices: <yellow>${it.staticServices} \n" +
                    "<gray>Maintenance: <yellow>${it.maintenance} \n" +
                    "<gray>PreferredNode: <yellow>${it.preferredNode} \n" +
                    "<gray>ServiceFactory: <yellow>${it.serviceFactoryName} \n" +
                    "<gray>CopyTemplatesToStatic: <yellow>${it.copyTemplatesToStatic} \n" +
                    "<gray>MinOnlineServices: <yellow>${it.minOnlineServices} \n" +
                    "<gray>MaxOnlineServices: <yellow>${it.maxOnlineServices} \n" +
                    "<gray>JvmArgs: <yellow>${it.jvmArgs.joinToString(", ")} \n" +
                    "<gray>EnvVars: <yellow>${it.envVars.joinToString(", ")} \n" +
                    "<gray>Attributes: <yellow>${it.attributes}"
            )
        }
    }

    @Permission("tasks.prepareServiceOnTask")
    @Command("task|tasks task <tasks> prepare")
    fun prepareService(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Flag("start") startService: Boolean,
        @Flag("amount") amount: Int?,
        @Flag("node") node: String?,
        @Flag("memory") memory: Int?,
        @Flag("startOrderedId") startOrderedId: Int?,
    ) {
        runBlocking {
            tasks.forEach { task ->
                val nodeName: String = node ?: task.preferredNode

                if (nodeName == Node.instance.configProvider.config.nodeName) {
                    Node.instance.localGrpcClient.tasksAPI.prepareServiceOnTask(
                        PrepareServiceOnTaskRequest.newBuilder()
                            .setTask(task.toDefinition())
                            .setAmount(amount ?: 1)
                            .setMemory(memory?.toLong() ?: task.maxMemory)
                            .setNodeName(nodeName)
                            .setStart(startService)
                            .setStartId(startOrderedId ?: 1)
                            .build()
                    )
                } else {
                    Node.instance.clusterProvider.remoteNodes
                        .find { it.endpoint.name == nodeName }
                        ?.let {
                            if (it.channel == null) {
                                source.sendMessage("Node ${it.endpoint.name} is not online!")
                                return@runBlocking
                            }
                            if (!it.getNode().isRunning()) {
                                source.sendMessage("Node ${it.endpoint.name} is not online!")
                                return@runBlocking
                            }
                            TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(it.channel!!)
                                .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                                .prepareServiceOnTask(
                                    PrepareServiceOnTaskRequest.newBuilder()
                                        .setTask(task.toDefinition())
                                        .setAmount(amount ?: 1)
                                        .setMemory(memory?.toLong() ?: task.maxMemory)
                                        .setNodeName(nodeName)
                                        .setStart(startService)
                                        .setStartId(startOrderedId ?: 1)
                                        .build()
                                )
                        }
                }
            }
        }
    }

    @Permission("tasks.delete")
    @Confirmation
    @Command("task|tasks task <tasks> delete")
    fun deleteTask(source: CommandSource, @Argument("tasks") tasks: List<Task>) {
        runBlocking {
            tasks.forEach { task ->
                val resp =
                    Node.instance.localGrpcClient.tasksAPI.deleteTask(
                        deleteTaskRequest { this.task = task.toDefinition() }
                    )
                source.sendMessage("Deleted task <white>${task.name}<dark_gray>.")
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> set maxMemory <memory>")
    fun setMaxMemory(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("memory") memory: Int,
    ) {
        runBlocking {
            tasks.forEach { task ->
                source.sendMessage("Setting max memory for task <white>${task.name} to <yellow>$memory MB")
                val newTask = task.copy(maxMemory = memory.toLong())
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> set maintenance <maintenance>")
    fun setMaintenance(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("maintenance") maintenance: Boolean,
    ) {
        runBlocking {
            tasks.forEach { task ->
                source.sendMessage("Setting maintenance for task <white>${task.name} to <yellow>$maintenance")
                val newTask = task.copy(maintenance = maintenance)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> set staticServices <static>")
    fun setStatic(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("static") static: Boolean,
    ) {
        runBlocking {
            tasks.forEach { task ->
                source.sendMessage("Setting staticServices for task <white>${task.name} to <yellow>$static")
                val newTask = task.copy(staticServices = static)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> set fallback <fallback>")
    fun setFallback(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("fallback") fallback: Boolean,
    ) {
        runBlocking {
            tasks.forEach { task ->
                source.sendMessage("Setting fallback for task <white>${task.name} to <yellow>$fallback")
                val newTask = task.copy(fallback = fallback)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> set preferredNode <node>")
    fun setPreferredNode(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("node") node: String,
    ) {
        runBlocking {
            tasks.forEach { task ->
                source.sendMessage("Setting preferredNode for task <white>${task.name} to <yellow>$node")
                val newTask = task.copy(preferredNode = node)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> set minServiceCount <count>")
    fun setMinServiceCount(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("count") count: Int,
    ) {
        runBlocking {
            tasks.forEach { task ->
                source.sendMessage("Setting minServiceCount for task <white>${task.name} to <yellow>$count")
                val newTask = task.copy(minOnlineServices = count)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }
}

object TaskCache {
    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build<String, List<TaskDefinition>>()

    fun getTasks(): List<TaskDefinition> {
        return cache.get("tasks") {
            runBlocking {
                Node.instance.localGrpcClient.tasksAPI.getAllTasks(getAllTasksRequest {}).tasksList
            }
        }
    }
}
