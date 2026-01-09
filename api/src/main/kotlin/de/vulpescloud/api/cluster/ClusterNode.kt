package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.node.v1.Node
import build.buf.gen.vulpescloud.node.v1.NodeStates
import build.buf.gen.vulpescloud.node.v1.node
import de.vulpescloud.api.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonString
import java.util.*

@Serializable
data class ClusterNode(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val grpcAddress: String,
    val grpcPort: Int,
    val state: NodeState,
    val maxMemory: Int,
    val head: Boolean,
    val bootTimestamp: Long,
) {
    fun toDefinition(): Node {
        return node {
            this.name = this@ClusterNode.name
            this.uuid = this@ClusterNode.uuid.toString()
            this.grpcAddress = this@ClusterNode.grpcAddress
            this.grpcPort = this@ClusterNode.grpcPort
            this.maxMemory = this@ClusterNode.maxMemory
            this.head = this@ClusterNode.head
            this.bootTimestamp = this@ClusterNode.bootTimestamp
            this.state =
                when (this@ClusterNode.state) {
                    NodeState.ONLINE -> NodeStates.NODE_STATES_ONLINE
                    NodeState.BOOTING -> NodeStates.NODE_STATES_BOOTING
                    NodeState.OFFLINE -> NodeStates.NODE_STATES_OFFLINE_UNSPECIFIED
                    NodeState.DRAINING -> NodeStates.NODE_STATES_DRAINING
                }
        }
    }

    fun toDocument(): BsonDocument {
        return BsonDocument().apply {
            append("name", BsonString(name))
            append("uuid", BsonString(uuid.toString()))
            append("grpcAddress", BsonString(grpcAddress))
            append("grpcPort", BsonString(grpcPort.toString()))
            append("maxMemory", BsonString(maxMemory.toString()))
            append("head", BsonString(head.toString()))
            append("bootTimestamp", BsonString(bootTimestamp.toString()))
            append("state", BsonString(state.name))
        }
    }

    companion object {
        fun fromDocument(document: BsonDocument): ClusterNode {
            return ClusterNode(
                document.getString("name").value,
                UUID.fromString(document.getString("uuid").value),
                document.getString("grpcAddress").value,
                document.getString("grpcPort").value.toInt(),
                NodeState.valueOf(document.getString("state").value),
                document.getString("maxMemory").value.toInt(),
                document.getString("head").value.toBoolean(),
                document.getString("bootTimestamp").value.toLong(),
            )
        }

        fun fromDefinition(definition: Node): ClusterNode {
            return ClusterNode(
                definition.name,
                UUID.fromString(definition.uuid),
                definition.grpcAddress,
                definition.grpcPort,
                when (definition.state) {
                    NodeStates.NODE_STATES_ONLINE -> NodeState.ONLINE
                    NodeStates.NODE_STATES_BOOTING -> NodeState.BOOTING
                    NodeStates.NODE_STATES_OFFLINE_UNSPECIFIED -> NodeState.OFFLINE
                    NodeStates.NODE_STATES_DRAINING -> NodeState.DRAINING
                    else -> NodeState.OFFLINE
                },
                definition.maxMemory,
                definition.head,
                definition.bootTimestamp,
            )
        }
    }
}
