package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeState
import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.api.events.cluster.NodeStateChangeEvent
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonString

object ClusterHelper {

    suspend fun updateNodeState(state: NodeState) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "nodes"
                )

        val filter = BsonDocument("name", BsonString(Node.instance.configProvider.config.nodeName))
        val node = getLocalNode()
        val newNode = node.copy(state = state)
        collection.replaceOne(filter, newNode.toDocument())

        EventsService.publish(
            EventSerializer.encode(
                NodeStateChangeEvent(getLocalNode(), NodeState.BOOTING, NodeState.ONLINE)
            )
        )
    }

    suspend fun getLocalNode(): ClusterNode {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "nodes"
                )

        val filter = BsonDocument("name", BsonString(Node.instance.configProvider.config.nodeName))
        val document = collection.find(filter).firstOrNull()
        if (document != null) {
            return ClusterNode.fromDocument(document)
        }
        val node =
            ClusterNode(
                Node.instance.configProvider.config.nodeName,
                Node.instance.configProvider.config.uuid,
                Node.instance.configProvider.config.grpcHost,
                Node.instance.configProvider.config.grpcPort,
                NodeState.ONLINE,
                Node.instance.configProvider.config.maxMemory,
                false,
                System.currentTimeMillis(),
            )
        collection.insertOne(node.toDocument())
        return node
    }

    suspend fun updateNode(node: ClusterNode) {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "nodes"
                )
        val filter = BsonDocument("name", BsonString(node.name))
        collection.replaceOne(filter, node.toDocument())
    }

    suspend fun getAllNodes(): List<ClusterNode> {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "nodes"
                )
        val nodes = mutableListOf<ClusterNode>()
        collection.find().collect { nodes.add(ClusterNode.fromDocument(it)) }
        return nodes.toList()
    }

    suspend fun getHeadNode(): ClusterNode? {
        return getAllNodes().find { it.state == NodeState.ONLINE && it.head }
    }
}
