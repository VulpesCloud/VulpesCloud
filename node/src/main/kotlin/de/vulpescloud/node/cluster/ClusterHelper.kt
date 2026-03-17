package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.api.events.cluster.NodeStateChangeEvent
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

object ClusterHelper {

    private val nodesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodes")
    }
    private val json = Json

    suspend fun updateNodeState(state: NodeState) {
        val node = getLocalNode()
        val newNode = node.copy(state = state)
        nodesDatabase.upsert(newNode.name, json.encodeToJsonElement(newNode))

        EventsService.publish(
            EventSerializer.encode(NodeStateChangeEvent(node, node.state, state)),
            true,
        )
    }

    suspend fun getLocalNode(): ClusterNode {
        nodesDatabase.get(Node.instance.configProvider.config.nodeName)?.let {
            return json.decodeFromJsonElement(ClusterNode.serializer(), it)
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
            )
        nodesDatabase.upsert(node.name, json.encodeToJsonElement(node))
        return node
    }

    suspend fun updateNode(node: ClusterNode) {
        nodesDatabase.upsert(node.name, json.encodeToJsonElement(node))
    }

    suspend fun getAllNodes(): List<ClusterNode> {
        return nodesDatabase.getAll().map {
            json.decodeFromJsonElement(ClusterNode.serializer(), it)
        }
    }

    suspend fun getHeadNode(): ClusterNode? {
        return getAllNodes().find { it.state == NodeState.ONLINE && it.head }
    }
}
