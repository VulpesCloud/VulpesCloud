package io.github.thecguygithub.node.task

import io.github.thecguygithub.api.tasks.Task
import io.github.thecguygithub.api.tasks.TaskProvider
import io.github.thecguygithub.api.version.VersionInfo
import java.util.*
import java.util.concurrent.CompletableFuture

class TaskProvider : TaskProvider() {

    private val tasks: MutableSet<Task> = mutableSetOf()

    override fun createAsync(
        name: String?,
        nodes: Array<String?>?,
        platform: VersionInfo?,
        maxMemory: Int,
        staticService: Boolean,
        minOnline: Int,
        maintenance: Boolean
    ): CompletableFuture<Optional<String?>?> {
        TODO("Not yet implemented")
    }

    override fun deleteAsync(task: String?): CompletableFuture<Optional<String?>?> {
        TODO("Not yet implemented")
    }

    override fun existsAsync(task: String?): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(tasks.stream().anyMatch { it.name().equals(task, true) })
    }

    override fun findAsync(task: String): CompletableFuture<Task?> {
        return CompletableFuture.completedFuture(tasks.stream().filter {it -> it.name().equals(task, true)}.findFirst().orElse(null))
    }

    override fun tasksAsync(): CompletableFuture<MutableSet<Task>>? {
        return CompletableFuture.completedFuture(tasks)
    }
}