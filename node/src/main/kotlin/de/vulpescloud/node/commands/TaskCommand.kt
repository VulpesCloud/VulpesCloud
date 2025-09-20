package de.vulpescloud.node.commands

import build.buf.gen.vulpescloud.services.v1.createServiceRequest
import build.buf.gen.vulpescloud.services.v1.startServiceRequest
import build.buf.gen.vulpescloud.tasks.v1.deleteTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotation.Alias
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

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
                    val regexPattern = input.readString()
                    regexPattern.replace("*", ".*")
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
                    " &8- &m$name &7Maintenance: &e${it.maintenance}&8, &7MaxPlayers: &e${it.maxPlayers}&8, &7MaxMemory: &e${it.maxMemory}MB&8, &7Static: &e${it.staticServices}&8, &7Fallback: &eNOT IMPLEMENTED&8, &7StartPort: &e${it.startPort}&8, &7Version: &e${it.software.name}-${it.software.version}"
                )
            }
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
                    Node.instance.localGrpcClient.serviceAPI.createService(
                        createServiceRequest { this.task = task.toDefinition() }
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
                Node.instance.localGrpcClient.tasksAPI
                source.sendMessage("ERR: Not yet implemented")
                // TODO: Implement Task update protobuf
            }
        }
    }
}
