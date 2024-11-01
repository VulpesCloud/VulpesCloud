package io.github.thecguygithub.api.tasks

import io.github.thecguygithub.api.version.VersionInfo
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class TaskProvider {

    abstract fun tasksAsync(): CompletableFuture<MutableSet<Task>>?

    @SneakyThrows
    fun tasks(): MutableSet<Task>? {
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
    ): CompletableFuture<Optional<String?>?>

    @SneakyThrows
    fun create(
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
    ): Optional<String?>? {
        return createAsync(name, templates, nodes, version, maxMemory, staticService, minOnlineCount, maintenance, maxPlayers, startPort, fallback)[5, TimeUnit.SECONDS]
    }

    abstract fun findAsync(task: String): CompletableFuture<Task?>

    @SneakyThrows
    fun findByName(task: String): Task? {
        return findAsync(task)[5, TimeUnit.SECONDS]
    }
}