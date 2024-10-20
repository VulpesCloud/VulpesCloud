package io.github.thecguygithub.node.task

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import java.util.*
import java.util.concurrent.CompletableFuture

class TaskProvider : ClusterTaskProvider() {

    private val tasks: MutableSet<ClusterTask> = mutableSetOf()

    override fun createAsync(
        name: String?,
        nodes: Array<String?>?,
        platform: PlatformGroupDisplay?,
        maxMemory: Int,
        staticService: Boolean,
        minOnline: Int,
        maintenance: Boolean,
    ): CompletableFuture<Optional<String?>?> {
        TODO("Not yet implemented")
    }

    override fun deleteAsync(task: String?): CompletableFuture<Optional<String?>?> {
        TODO("Not yet implemented")
    }

    override fun existsAsync(task: String?): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(tasks.stream().anyMatch { it.name().equals(task, true) })
    }

    override fun findAsync(task: String): CompletableFuture<ClusterTask?> {
        return CompletableFuture.completedFuture(tasks.stream().filter {it -> it.name().equals(task, true)}.findFirst().orElse(null))
    }

    override fun tasksAsync(): CompletableFuture<MutableSet<ClusterTask>>? {
        return CompletableFuture.completedFuture(tasks)
    }
}