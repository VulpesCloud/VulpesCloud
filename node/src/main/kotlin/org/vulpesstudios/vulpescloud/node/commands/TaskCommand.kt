/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.commands

import build.buf.gen.vulpescloud.tasks.v1.*
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.processors.confirmation.annotation.Confirmation
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.command.ConsoleCommandSource
import org.vulpesstudios.vulpescloud.node.command.annotation.Alias
import org.vulpesstudios.vulpescloud.node.command.annotation.SpecificCommandSource
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor
import org.vulpesstudios.vulpescloud.node.setup.setups.TaskSetup
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

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
    @SpecificCommandSource(ConsoleCommandSource::class)
    fun setupTask(source: CommandSource) {
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

            source.sendMessage(
                "<gray>The following</gray> <gold>${tasks.size}</gold> <gray>task(s) are registered:</gray>"
            )
            tasks.forEach {
                val name = it.name.padEnd(maxNameLength)
                source.sendMessage(
                    " <dark_gray>»</dark_gray> <white>$name</white> <dark_gray>| <gray>Maintenance:</gray> <white>${it.maintenance}</white> <dark_gray>| <gray>MaxPlayers:</gray> <white>${it.maxPlayers}</white> <dark_gray>| <gray>MaxMemory:</gray> <white>${it.maxMemory}MB</white> <dark_gray>| <gray>Static:</gray> <white>${it.staticServices}</white> <dark_gray>| <gray>Fallback:</gray> <white>${it.fallback}</white> <dark_gray>| <gray>StartPort:</gray> <white>${it.startPort}</white> <dark_gray>| <gray>Version:</gray> <white>${it.software.name}-${it.software.version}</white>"
                )
            }
        }
    }

    @Permission("tasks.get")
    @Command("task|tasks task <tasks> info")
    fun infoTask(source: CommandSource, @Argument("tasks") tasks: List<Task>) {
        tasks.forEach {
            source.sendMessage(
                "<gold>---------</gold> <white>${it.name}</white> <gold>---------</gold>\n" +
                    "<gray>MaxPlayers<dark_gray>:</dark_gray> <white>${it.maxPlayers}</white> \n" +
                    "<gray>MaxMemory<dark_gray>:</dark_gray> <white>${it.maxMemory}MB</white> \n" +
                    "<gray>Static<dark_gray>:</dark_gray> <white>${it.staticServices}</white> \n" +
                    "<gray>Fallback<dark_gray>:</dark_gray> <white>${it.fallback}</white> \n" +
                    "<gray>StartPort<dark_gray>:</dark_gray> <white>${it.startPort}</white> \n" +
                    "<gray>Version<dark_gray>:</dark_gray> <white>${it.software.name}-${it.software.version}</white> \n" +
                    "<gray>Maintenance<dark_gray>:</dark_gray> <white>${it.maintenance}</white> \n" +
                    "<gray>PreferredNode<dark_gray>:</dark_gray> <white>${it.preferredNodes.joinToString()}</white> \n" +
                    "<gray>ServiceFactory<dark_gray>:</dark_gray> <white>${it.serviceFactoryName}</white> \n" +
                    "<gray>CopyTemplates<dark_gray>:</dark_gray> <white>${it.copyTemplatesToStatic}</white> \n" +
                    "<gray>MinOnline<dark_gray>:</dark_gray> <white>${it.minOnlineServices}</white> \n" +
                    "<gray>MaxOnline<dark_gray>:</dark_gray> <white>${it.maxOnlineServices}</white> \n" +
                    "<gray>JvmArgs<dark_gray>:</dark_gray> <white>${it.jvmArgs.joinToString(", ")}</white> \n" +
                    "<gray>EnvVars<dark_gray>:</dark_gray> <white>${it.envVars.joinToString(", ")}</white> \n" +
                    "<gray>Attributes<dark_gray>:</dark_gray> <white>${it.attributes}</white>"
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
            val nodeSnapshots =
                ClusterHelper.getAllNodeSnapshots().let { snapshots ->
                    if (
                        snapshots.none { it.name == Node.instance.configProvider.config.nodeName }
                    ) {
                        snapshots + ClusterHelper.getLocalNodeSnapshot()
                    } else {
                        snapshots
                    }
                }

            tasks.forEach { task ->
                val requiredMemory = memory?.toLong() ?: task.maxMemory
                val nodeName: String =
                    if (node != null) {
                        val targetSnapshot = nodeSnapshots.find { it.name == node }
                        if (targetSnapshot == null) {
                            source.sendMessage("<red>Node $node was not found!</red>")
                            return@forEach
                        }
                        if (targetSnapshot.services.memoryAvailable < requiredMemory) {
                            source.sendMessage(
                                "<red>Node $node does not have enough memory to start the service!</red>"
                            )
                            return@forEach
                        }
                        node
                    } else {
                        val bestNode =
                            nodeSnapshots
                                .filter { it.name in task.preferredNodes }
                                .filter { it.state == NodeState.ONLINE }
                                .filter { it.services.memoryAvailable >= requiredMemory }
                                .maxByOrNull { it.services.memoryAvailable }

                        if (bestNode == null) {
                            source.sendMessage(
                                "<red>No eligible node found for task ${task.name}!</red>"
                            )
                            return@forEach
                        }
                        bestNode.name
                    }

                if (nodeName == Node.instance.configProvider.config.nodeName) {
                    Node.instance.localGrpcClient.tasksAPI.prepareServiceOnTask(
                        PrepareServiceOnTaskRequest.newBuilder()
                            .setTask(task.toDefinition())
                            .setAmount(amount ?: 1)
                            .setMemory(requiredMemory)
                            .setNodeName(nodeName)
                            .setStart(startService)
                            .setStartId(startOrderedId ?: 1)
                            .build()
                    )
                } else {
                    val remoteNode =
                        Node.instance.clusterProvider.remoteNodes.find {
                            it.endpoint.name == nodeName
                        }

                    if (remoteNode == null) {
                        source.sendMessage("<red>Node $nodeName was not found!</red>")
                        return@forEach
                    }
                    if (remoteNode.channel == null) {
                        source.sendMessage(
                            "<red>Node ${remoteNode.endpoint.name} is not online!</red>"
                        )
                        return@forEach
                    }
                    if (remoteNode.getSnapshot().state != NodeState.ONLINE) {
                        source.sendMessage(
                            "<red>Node ${remoteNode.endpoint.name} is not online!</red>"
                        )
                        return@forEach
                    }
                    TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(remoteNode.channel!!)
                        .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                        .prepareServiceOnTask(
                            PrepareServiceOnTaskRequest.newBuilder()
                                .setTask(task.toDefinition())
                                .setAmount(amount ?: 1)
                                .setMemory(requiredMemory)
                                .setNodeName(nodeName)
                                .setStart(startService)
                                .setStartId(startOrderedId ?: 1)
                                .build()
                        )
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
                source.sendMessage(
                    "<gray>Deleted task</gray> <white>${task.name}</white><dark_gray>.</dark_gray>"
                )
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
                source.sendMessage(
                    "<gray>Setting max memory for task</gray> <white>${task.name}</white> <gray>to</gray> <gold>$memory MB</gold>"
                )
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
                source.sendMessage(
                    "<gray>Setting maintenance for task</gray> <white>${task.name}</white> <gray>to</gray> <white>$maintenance</white>"
                )
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
                source.sendMessage(
                    "<gray>Setting staticServices for task</gray> <white>${task.name}</white> <gray>to</gray> <white>$static</white>"
                )
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
                source.sendMessage(
                    "<gray>Setting fallback for task</gray> <white>${task.name}</white> <gray>to</gray> <white>$fallback</white>"
                )
                val newTask = task.copy(fallback = fallback)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> add preferredNode <node>")
    fun addPreferredNode(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("node") node: String,
    ) {
        runBlocking {
            tasks.forEach { task ->
                if (task.preferredNodes.contains(node)) {
                    source.sendMessage(
                        "<red>Node</red> <white>$node</white> <red>is already a preferred node for task</red> <white>${task.name}</white>"
                    )
                    return@forEach
                }
                source.sendMessage(
                    "<gray>Adding preferredNode</gray> <white>$node</white> <gray>to task</gray> <white>${task.name}</white>"
                )
                val newTask = task.copy(preferredNodes = task.preferredNodes + node)
                Node.instance.localGrpcClient.tasksAPI.updateTask(
                    updateTaskRequest { this.task = newTask.toDefinition() }
                )
            }
        }
    }

    @Permission("tasks.update")
    @Command("task|tasks task <tasks> remove preferredNode <node>")
    fun removePreferredNode(
        source: CommandSource,
        @Argument("tasks") tasks: List<Task>,
        @Argument("node") node: String,
    ) {
        runBlocking {
            tasks.forEach { task ->
                if (!task.preferredNodes.contains(node)) {
                    source.sendMessage(
                        "<red>Node</red> <white>$node</white> <red>is not a preferred node for task</red> <white>${task.name}</white>"
                    )
                    return@forEach
                }
                source.sendMessage(
                    "<gray>Removing preferredNode</gray> <white>$node</white> <gray>from task</gray> <white>${task.name}</white>"
                )
                val newTask = task.copy(preferredNodes = task.preferredNodes - node)
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
                source.sendMessage(
                    "<gray>Setting minServiceCount for task</gray> <white>${task.name}</white> <gray>to</gray> <gold>$count</gold>"
                )
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
