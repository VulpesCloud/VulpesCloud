package io.github.thecguygithub.api.services

import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


abstract class ClusterServiceProvider {

    @SneakyThrows
    fun services(): MutableList<ClusterService>? {
        return servicesAsync()?.get(5, TimeUnit.SECONDS)
    }

    abstract fun servicesAsync(): CompletableFuture<MutableList<ClusterService>>?


    @SneakyThrows
    fun findByID(id: UUID?): ClusterService? {
        return findAsync(id)!!.get(5, TimeUnit.SECONDS)
    }

    abstract fun findAsync(id: UUID?): CompletableFuture<ClusterService?>?

    @SneakyThrows
    fun findByName(name: String?): ClusterService? {
        return findAsync(name)!!.get(5, TimeUnit.SECONDS)
    }

    abstract fun findAsync(name: String?): CompletableFuture<ClusterService?>?

    @SneakyThrows
    fun findByFilter(filter: ClusterServiceFilter): List<ClusterService?>? {
        return findAsync(filter)[5, TimeUnit.SECONDS]
    }

    abstract fun findAsync(filter: ClusterServiceFilter): CompletableFuture<List<ClusterService?>?>

    abstract fun factory(): ClusterServiceFactory?

}