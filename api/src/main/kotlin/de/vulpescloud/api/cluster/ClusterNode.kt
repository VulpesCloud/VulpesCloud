package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.node.v1.Node
import build.buf.gen.vulpescloud.node.v1.NodeStates
import build.buf.gen.vulpescloud.node.v1.node
import de.vulpescloud.api.serializer.UUIDSerializer
import java.util.*
import kotlinx.serialization.Serializable

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
    val attributes: Map<String, String>,
) {

    fun isRunning(): Boolean {
        return state == NodeState.ONLINE || state == NodeState.DRAINING
    }

    fun toDefinition(): Node {
        return node {
            this.name = this@ClusterNode.name
            this.uuid = this@ClusterNode.uuid.toString()
            this.grpcAddress = this@ClusterNode.grpcAddress
            this.grpcPort = this@ClusterNode.grpcPort
            this.maxMemory = this@ClusterNode.maxMemory
            this.head = this@ClusterNode.head
            this.bootTimestamp = this@ClusterNode.bootTimestamp
            this.state = this@ClusterNode.state.toNodeStates()
            this.attributes.putAll(this@ClusterNode.attributes)
        }
    }

    companion object {
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
                definition.attributesMap,
            )
        }
    }
}
