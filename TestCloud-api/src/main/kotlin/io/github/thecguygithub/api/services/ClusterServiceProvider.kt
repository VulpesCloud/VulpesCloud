package io.github.thecguygithub.api.services

import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class ClusterServiceProvider {
    @SneakyThrows
    fun services(): List<ClusterService?>? {
        return servicesAsync()[5, TimeUnit.SECONDS]
    }

    abstract fun servicesAsync(): CompletableFuture<List<ClusterService?>?>


    @SneakyThrows
    fun find(id: UUID?): ClusterService? {
        return findAsync(id)!!.get(5, TimeUnit.SECONDS)
    }

    abstract fun findAsync(id: UUID?): CompletableFuture<ClusterService?>?

    @SneakyThrows
    fun find(name: String?): ClusterService? {
        return findAsync(name)!!.get(5, TimeUnit.SECONDS)
    }

    abstract fun findAsync(name: String?): CompletableFuture<ClusterService?>?

    @SneakyThrows
    fun find(filter: ClusterServiceFilter?): List<ClusterService?>? {
        return findAsync(filter)[5, TimeUnit.SECONDS]
    }

    abstract fun findAsync(filter: ClusterServiceFilter?): CompletableFuture<List<ClusterService?>?>

    abstract fun factory(): ClusterServiceFactory?

    fun write(value: ClusterService) {
        // buffer.writeUniqueId(value.id())
        // buffer.writeInt(value.orderedId())
        // buffer.writeString(value.hostname())
        // buffer.writeInt(value.port())
        // buffer.writeInt(value.maxPlayers())
        // buffer.writeString(value.runningNode())
        // buffer.writeEnum(value.state())

        // we add also all group information
        // CloudAPI.instance().groupProvider().write(value.group(), buffer)

        // sync properties
        // PropertiesBuffer.write(value.properties(), buffer)
    }
}