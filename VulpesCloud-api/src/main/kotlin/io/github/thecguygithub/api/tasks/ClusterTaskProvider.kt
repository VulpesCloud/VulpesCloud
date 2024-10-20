package io.github.thecguygithub.api.tasks

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class ClusterTaskProvider {

    abstract fun tasksAsync(): CompletableFuture<MutableSet<ClusterTask>>?

    @SneakyThrows
    fun tasks(): MutableSet<ClusterTask>? {
        return tasksAsync()?.get(5, TimeUnit.SECONDS)
    }

    abstract fun existsAsync(task: String?): CompletableFuture<Boolean>

    @SneakyThrows
    fun exists(task: String?): Boolean {
        return existsAsync(task)[5, TimeUnit.SECONDS]
    }

    abstract fun deleteAsync(task: String?): CompletableFuture<Optional<String?>?>

    @SneakyThrows
    fun delete(task: String?): Optional<String?>? {
        return deleteAsync(task)[5, TimeUnit.SECONDS]
    }

    abstract fun createAsync(
        name: String?,
        nodes: Array<String?>?,
        platform: PlatformGroupDisplay?,
        maxMemory: Int,
        staticService: Boolean,
        minOnline: Int,
        maintenance: Boolean
    ): CompletableFuture<Optional<String?>?>

    @SneakyThrows
    fun create(
        name: String?,
        nodes: Array<String?>?,
        platform: PlatformGroupDisplay?,
        maxMemory: Int,
        staticService: Boolean,
        minOnline: Int,
        maintenance: Boolean
    ): Optional<String?>? {
        return createAsync(name, nodes, platform, maxMemory, staticService, minOnline, maintenance)[5, TimeUnit.SECONDS]
    }

    abstract fun findAsync(task: String): CompletableFuture<ClusterTask?>

    @SneakyThrows
    fun findByName(task: String): ClusterTask? {
        return findAsync(task)[5, TimeUnit.SECONDS]
    }
}