package io.github.thecguygithub.api.tasks

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class ClusterTaskProvider {

    abstract fun groupsAsync(): CompletableFuture<MutableSet<ClusterTask?>?>

    @SneakyThrows
    fun groups(): MutableSet<ClusterTask?>? {
        return groupsAsync()[5, TimeUnit.SECONDS]
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
        maxOnline: Int
    ): CompletableFuture<Optional<String?>?>

    @SneakyThrows
    fun create(
        name: String?,
        nodes: Array<String?>?,
        platform: PlatformGroupDisplay?,
        maxMemory: Int,
        staticService: Boolean,
        minOnline: Int,
        maxOnline: Int
    ): Optional<String?>? {
        return createAsync(name, nodes, platform, maxMemory, staticService, minOnline, maxOnline)[5, TimeUnit.SECONDS]
    }

    abstract fun findAsync(group: String): CompletableFuture<ClusterTask?>

    @SneakyThrows
    fun find(group: String): ClusterTask? {
        return findAsync(group)[5, TimeUnit.SECONDS]
    }

    fun write(group: ClusterTask) {
        // buffer.writeString(group.name())
        // buffer.writeInt(group.maxMemory())
        // buffer.writeInt(group.maxPlayers())
        // buffer.writeInt(group.minOnlineCount())
        // buffer.writeBoolean(group.staticService())

        // buffer.writeString(group.platform()!!.platform)
        // buffer.writeString(group.platform()!!.version)
        // buffer.writeEnum(group.platform()!!.type)

        // buffer.writeInt(group.nodes()!!.size)
        // for (node in group.nodes()!!) {
        //     buffer.writeString(node)
        // }

        // buffer.writeInt(group.templates()!!.size)
        // for (template in group.templates()!!) {
        //     buffer.writeString(template)
        // }
    }
}