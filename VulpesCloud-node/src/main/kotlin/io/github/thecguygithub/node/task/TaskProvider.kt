package io.github.thecguygithub.node.task

import io.github.thecguygithub.api.tasks.Task
import io.github.thecguygithub.api.tasks.TaskProvider
import io.github.thecguygithub.api.version.VersionInfo
import io.github.thecguygithub.node.Node
import java.util.*
import java.util.concurrent.CompletableFuture

class TaskProvider : TaskProvider() {

    private val tasks: MutableSet<Task> = TaskFactory.readGroups().toMutableSet()

    override fun createAsync(
        name: String,
        templates: List<String?>,
        nodes: List<String?>,
        version: VersionInfo,
        maxMemory: Int,
        staticService: Boolean,
        minOnlineCount: Int,
        maintenance: Boolean,
        maxPlayers: Int,
        startPort: Int,
        fallback: Boolean
    ): CompletableFuture<Optional<String?>?> {
        val taskFuture = CompletableFuture<Optional<String?>?>()
        val task = TaskImpl(
            name,
            maxMemory,
            version,
            templates,
            nodes,
            maxPlayers,
            staticService,
            minOnlineCount,
            maintenance,
            startPort,
            fallback
        )
        Node.instance?.getRC()?.sendMessage("TASK;CREATE;${task}", "vulpescloud-event-task-update")
        return taskFuture
    }

    override fun deleteAsync(task: String?): CompletableFuture<Optional<String?>?> {
        val taskFuture = CompletableFuture<Optional<String?>?>()
        Node.instance!!.getRC()?.sendMessage("TASK;DELETE;$task", "vulpescloud-event-task-update")
        return taskFuture
    }

    override fun existsAsync(task: String?): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(tasks.stream().anyMatch { it.name().equals(task, true) })
    }

    override fun findAsync(task: String): CompletableFuture<Task?> {
        return CompletableFuture.completedFuture(tasks.stream().filter { it.name().equals(task, true)}.findFirst().orElse(null))
    }

    override fun tasksAsync(): CompletableFuture<MutableSet<Task>>? {
        return CompletableFuture.completedFuture(tasks)
    }
}