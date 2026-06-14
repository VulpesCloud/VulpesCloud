package de.vulpescloud.node.cluster

import build.buf.gen.vulpescloud.events.v1.nodeStateChangeEvent
import com.github.benmanes.caffeine.cache.Caffeine
import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory

object ClusterHelper {

    private val nodesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodes")
    }
    private val json = Json

    private val cache =
        Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build<String, ClusterNode>()
    private val logger = LoggerFactory.getLogger(ClusterHelper::class.java)

    suspend fun isLocalNodeInDatabase(): Boolean {
        return nodesDatabase.get(Node.instance.configProvider.config.nodeName) != null
    }

    suspend fun hasDatabaseBeenInitialized(): Boolean {
        return nodesDatabase.getAll().isNotEmpty()
    }

    suspend fun updateNodeState(state: NodeState) {
        val node = getLocalNode()

        val newNode = node.copy(state = state)
        nodesDatabase.upsert(newNode.name, json.encodeToJsonElement(newNode))
        cache.put(newNode.name, newNode)

        EventsService.publish(
            nodeStateChangeEvent {
                this.node = node.toDefinition()
                this.oldState = node.state.toNodeStates()
                this.newState = state.toNodeStates()
            },
            true,
        )
    }

    suspend fun getLocalNode(): ClusterNode {
        val name = Node.instance.configProvider.config.nodeName

        cache.getIfPresent(name)?.let {
            return it
        }

        nodesDatabase.get(name)?.let {
            val node = json.decodeFromJsonElement(ClusterNode.serializer(), it)
            cache.put(name, node)
            return node
        }

        val node =
            ClusterNode(
                Node.instance.configProvider.config.nodeName,
                Node.instance.configProvider.config.uuid,
                Node.instance.configProvider.config.grpcHost,
                Node.instance.configProvider.config.grpcPort,
                NodeState.OFFLINE,
                Node.instance.configProvider.config.maxMemory,
                false,
                System.currentTimeMillis(),
                emptyMap(),
            )
        nodesDatabase.upsert(node.name, json.encodeToJsonElement(node))
        cache.put(node.name, node)
        return node
    }

    suspend fun updateNode(node: ClusterNode) {
        nodesDatabase.upsert(node.name, json.encodeToJsonElement(node))
        cache.put(node.name, node)
    }

    suspend fun getAllNodes(): List<ClusterNode> {
        return nodesDatabase.getAll().map {
            val node = json.decodeFromJsonElement(ClusterNode.serializer(), it)
            cache.put(node.name, node)
            node
        }
    }

    suspend fun getHeadNode(): ClusterNode? {
        return getAllNodes().find { it.state == NodeState.ONLINE && it.head }
    }

    suspend fun getNode(name: String): ClusterNode? {
        cache.getIfPresent(name)?.let {
            return it
        }

        nodesDatabase.get(name)?.let {
            val node = json.decodeFromJsonElement(ClusterNode.serializer(), it)
            cache.put(name, node)
            return node
        }

        return null
    }
}
