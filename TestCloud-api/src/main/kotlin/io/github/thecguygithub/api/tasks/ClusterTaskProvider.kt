package io.github.thecguygithub.api.tasks

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class ClusterTaskProvider {

    abstract fun groupsAsync(): CompletableFuture<MutableSet<ClusterTask>>?

    @SneakyThrows
    fun groups(): MutableSet<ClusterTask>? {
        return groupsAsync()?.get(5, TimeUnit.SECONDS)
    }

    abstract fun existsAsync(group: String?): CompletableFuture<Boolean>

    @SneakyThrows
    fun exists(group: String?): Boolean {
        return existsAsync(group)[5, TimeUnit.SECONDS]
    }

    abstract fun deleteAsync(group: String?): CompletableFuture<Optional<String?>?>

    @SneakyThrows
    fun delete(group: String?): Optional<String?>? {
        return deleteAsync(group)[5, TimeUnit.SECONDS]
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

    abstract fun findAsync(group: String): CompletableFuture<ClusterTask?>

    @SneakyThrows
    fun find(group: String): ClusterTask? {
        return findAsync(group)[5, TimeUnit.SECONDS]
    }
}