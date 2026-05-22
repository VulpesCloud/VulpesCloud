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
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.setup.setups.TaskSetup
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
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
        Node.instance.setupProvider.startSetup(TaskSetup())
    }

    @Command("task|tasks list")
    fun listTasks(source: CommandSource) {
        NodeCoroutineScope.launch {
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
                    " &8- &m$name &7Maintenance: &e${it.maintenance}&8, &7MaxPlayers: &e${it.maxPlayers}&8, &7MaxMemory: &e${it.maxMemory}MB&8, &7Static: &e${it.staticServices}&8, &7Fallback: &e${it.fallback}&8, &7StartPort: &e${it.startPort}&8, &7Version: &e${it.software.name}-${it.software.version}"
                )
            }
        }
    }

    @Command("task|tasks task <tasks> info")
    fun infoTask(source: CommandSource, @Argument("tasks") tasks: List<Task>) {
        tasks.forEach {
            source.sendMessage(
                "&7Name: &e${it.name} \n" +
                    "&7MaxPlayers: &e${it.maxPlayers} \n" +
                    "&7MaxMemory: &e${it.maxMemory}MB \n" +
                    "&7Static: &e${it.staticServices} \n" +
                    "&7Fallback: &e${it.fallback} \n" +
                    "&7StartPort: &e${it.startPort} \n" +
                    "&7Version: &e${it.software.name}-${it.software.version} \n" +
                    "&7StaticServices: &e${it.staticServices} \n" +
                    "&7Maintenance: &e${it.maintenance} \n" +
                    "&7PreferredNode: &e${it.preferredNode} \n" +
                    "&7ServiceFactory: &e${it.serviceFactoryName} \n" +
                    "&7CopyTemplatesToStatic: &e${it.copyTemplatesToStatic} \n" +
                    "&7MinOnlineServices: &e${it.minOnlineServices} \n" +
                    "&7MaxOnlineServices: &e${it.maxOnlineServices} \n" +
                    "&7JvmArgs: &e${it.jvmArgs.joinToString(", ")} \n" +
                    "&7EnvVars: &e${it.envVars.joinToString(", ")} \n" +
                    "&7Attributes: &e${it.attributes}"
            )
        }
    }

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
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                if (node == Node.instance.configProvider.config.nodeName || node == null) {
                    Node.instance.localGrpcClient.tasksAPI.prepareServiceOnTask(
                        PrepareServiceOnTaskRequest.newBuilder()
                            .setTask(task.toDefinition())
                            .setAmount(amount ?: 1)
                            .setMemory(memory?.toLong() ?: task.maxMemory)
                            .setNodeName(node ?: Node.instance.configProvider.config.nodeName)
                            .setStart(startService)
                            .setStartId(startOrderedId ?: 1)
                            .build()
                    )
                } else {
                    Node.instance.clusterProvider.remoteNodes
                        .find { it.endpoint.name == node }
                        ?.let {
                            if (it.channel == null) {
                                source.sendMessage("Node ${it.endpoint.name} is not online!")
                                return@launch
                            }
                            if (!it.getNode().isRunning()) {
                                source.sendMessage("Node ${it.endpoint.name} is not online!")
                                return@launch
                            }
                            TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(it.channel!!).prepareServiceOnTask(
                                PrepareServiceOnTaskRequest.newBuilder()
                                    .setTask(task.toDefinition())
                                    .setAmount(amount ?: 1)
                                    .setMemory(memory?.toLong() ?: task.maxMemory)
                                    .setNodeName(node)
                                    .setStart(startService)
                                    .setStartId(startOrderedId ?: 1)
                                    .build()
                            )
                        }
                }
            }
        }
    }

    @Confirmation
    @Command("task|tasks task <tasks> delete")
    fun deleteTask(source: CommandSource, @Argument("tasks") tasks: List<Task>) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                val resp =
                    Node.instance.localGrpcClient.tasksAPI.deleteTask(
                        deleteTaskRequest { this.task = task.toDefinition() }
                    )
                source.sendMessage("Deleted task &m${task.name}&8.")
            }
        }
    }

    @Command("task|tasks task <tasks> set maxMemory <memory>")
    fun setMaxMemory(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("memory") memory: Int,
    ) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                source.sendMessage("Setting max memory for task &m${task.name} to &e$memory MB")
                val newTask = task.copy(maxMemory = memory.toLong())
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Command("task|tasks task <tasks> set maintenance <maintenance>")
    fun setMaintenance(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("maintenance") maintenance: Boolean,
    ) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                source.sendMessage("Setting maintenance for task &m${task.name} to &e$maintenance")
                val newTask = task.copy(maintenance = maintenance)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Command("task|tasks task <tasks> set staticServices <static>")
    fun setStatic(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("static") static: Boolean,
    ) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                source.sendMessage("Setting staticServices for task &m${task.name} to &e$static")
                val newTask = task.copy(staticServices = static)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Command("task|tasks task <tasks> set fallback <fallback>")
    fun setFallback(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("fallback") fallback: Boolean,
    ) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                source.sendMessage("Setting fallback for task &m${task.name} to &e$fallback")
                val newTask = task.copy(fallback = fallback)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Command("task|tasks task <tasks> set preferredNode <node>")
    fun setPreferredNode(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("node") node: String,
    ) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                source.sendMessage("Setting preferredNode for task &m${task.name} to &e$node")
                val newTask = task.copy(preferredNode = node)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Command("task|tasks task <tasks> set minServiceCount <count>")
    fun setMinServiceCount(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("count") count: Int,
    ) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                source.sendMessage("Setting minServiceCount for task &m${task.name} to &e$count")
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
