package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.prepareServiceByTaskRequest
import build.buf.gen.vulpescloud.services.v1.startServiceRequest
import build.buf.gen.vulpescloud.tasks.v1.deleteTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import build.buf.gen.vulpescloud.tasks.v1.updateTaskRequest
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import de.vulpescloud.node.setup.setups.TaskSetup
import java.util.concurrent.CompletableFuture
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
        return CompletableFuture.supplyAsync {
                runBlocking {
                    Node.instance.localGrpcClient.tasksAPI
                        .getAllTasks(getAllTasksRequest {})
                        .tasksList
                        .map { it.name }
                }
            }
            .thenApply { it.stream() }
            .exceptionally { Stream.empty() }
            .get(5, TimeUnit.SECONDS)
    }

    @Parser(suggestions = "tasks")
    fun taskParser(input: CommandInput): List<Task> {
        return CompletableFuture.supplyAsync {
                runBlocking {
                    val regexPattern = input.readString().replace("*", ".*")
                    val regex = Regex(regexPattern)

                    Node.instance.localGrpcClient.tasksAPI
                        .getAllTasks(getAllTasksRequest {})
                        .tasksList
                        .filter { regex.matches(it.name) }
                        .map { Task.fromDefinition(it) }
                }
            }
            .thenApply { it }
            .exceptionally { throw it }
            .get(5, TimeUnit.SECONDS)
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
                    "&7Version: &e${it.software.name}-${it.software.version}" +
                    "&7StaticServices: &e${it.staticServices} \n" +
                    "&7Maintenance: &e${it.maintenance}" +
                    "&7PreferredNode: &e${it.preferredNode}" +
                    "&7ServiceFactory: &e${it.serviceFactoryName}" +
                    "&7CopyTemplatesToStatic: &e${it.copyTemplatesToStatic}" +
                    "&7MinOnlineServices: &e${it.minOnlineServices} \n" +
                    "&7MaxOnlineServices: &e${it.maxOnlineServices} \n" +
                    "&7JvmArgs: &e${it.jvmArgs.joinToString(", ")} \n" +
                    "&7EnvVars: &e${it.envVars.joinToString(", ")}" +
                    "&7Attributes: &e${it.attributes?.toString() ?: "None"}"
            )
        }
    }

    @Command("task|tasks task <tasks> prepare")
    fun prepareService(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Flag("start") startService: Boolean,
    ) {
        NodeCoroutineScope.launch {
            tasks.forEach { task ->
                val resp =
                    Node.instance.localGrpcClient.serviceAPI.prepareServiceByTask(
                        prepareServiceByTaskRequest { this.task = task.toDefinition() }
                    )
                source.sendMessage("Prepared service for task &m${task.name}&8.")
                if (startService) {
                    Node.instance.localGrpcClient.serviceAPI.startService(
                        startServiceRequest { this.service = resp.service }
                    )

                    source.sendMessage(
                        "Started service &m${task.name}-${resp.service.orderedId}&8."
                    )
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
