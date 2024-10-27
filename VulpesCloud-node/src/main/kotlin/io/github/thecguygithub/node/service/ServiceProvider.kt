package io.github.thecguygithub.node.service

import io.github.thecguygithub.api.services.*
import io.github.thecguygithub.api.version.VersionType
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.redis.RedisManager
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

class ServiceProvider : ClusterServiceProvider() {

    private val services: MutableList<ClusterService> = CopyOnWriteArrayList()
    private val factory = ServiceFactory()
    private val logger = Logger()
    private val channels = mutableListOf("testcloud-service-actions", "testcloud-service-events")
    private val redisManger = Node.instance?.getRC()?.let { RedisManager(it.getJedisPool()) }


    override fun factory(): ClusterServiceFactory {
        return factory
    }

    override fun findAsync(id: UUID?): CompletableFuture<ClusterService?>? {
        return CompletableFuture.completedFuture(services.find { it.id() == id })
    }

    override fun findAsync(name: String?): CompletableFuture<ClusterService?>? {
        return CompletableFuture.completedFuture(services.find { it.name() == name })
    }

    override fun findAsync(filter: ClusterServiceFilter?): CompletableFuture<List<ClusterService?>?> {
        return CompletableFuture.completedFuture(
            when (filter) {
                ClusterServiceFilter.ONLINE_SERVICES -> services.filter { it.state(ClusterServiceStates.STARTING) == ClusterServiceStates.ONLINE }
                ClusterServiceFilter.EMPTY_SERVICES -> services.filter { it.isEmpty() }
                ClusterServiceFilter.PLAYERS_PRESENT_SERVERS -> services.filter { !it.isEmpty() }
                ClusterServiceFilter.SAME_NODE_SERVICES -> services.filter {
                    Node.nodeConfig?.name == it.runningNode()
                }
                ClusterServiceFilter.PROXIES -> services.filter { it.task().version().type == VersionType.PROXY }
                ClusterServiceFilter.SERVERS -> services.filter { it.task().version().type == VersionType.SERVER }

                null -> null
                ClusterServiceFilter.FALLBACKS -> null
                ClusterServiceFilter.LOWEST_FALLBACK -> null
            }
        )
    }

    override fun servicesAsync(): CompletableFuture<MutableList<ClusterService>>? {
        return CompletableFuture.completedFuture(services)
    }

}