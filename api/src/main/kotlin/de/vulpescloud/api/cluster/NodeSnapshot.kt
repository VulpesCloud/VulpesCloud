package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.node.v1.NodeSnapshot
import build.buf.gen.vulpescloud.node.v1.NodeStates
import build.buf.gen.vulpescloud.node.v1.nodeSnapshot
import de.vulpescloud.api.serializer.UUIDSerializer
import java.util.*
import kotlinx.serialization.Serializable
import org.bson.*

@Serializable
data class NodeSnapshot(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val state: NodeState,
    val usedMemory: Int,
    val cpuUsage: Double,
    val onlinePlayers: Int,
    val timestamp: Long,
    val locked: Boolean,
) {

    fun toDefinition(): NodeSnapshot {
        return nodeSnapshot {
            this.name = this@NodeSnapshot.name
            this.uuid = this@NodeSnapshot.uuid.toString()
            this.usedMemory = this@NodeSnapshot.usedMemory
            this.onlinePlayers = this@NodeSnapshot.onlinePlayers
            this.timestamp = this@NodeSnapshot.timestamp
            this.state = this@NodeSnapshot.state.toNodeStates()
            this.locked = this@NodeSnapshot.locked
        }
    }

    companion object {
        fun fromDefinition(definition: NodeSnapshot): de.vulpescloud.api.cluster.NodeSnapshot {
            return NodeSnapshot(
                definition.name,
                UUID.fromString(definition.uuid),
                when (definition.state) {
                    NodeStates.NODE_STATES_ONLINE -> NodeState.ONLINE
                    NodeStates.NODE_STATES_BOOTING -> NodeState.BOOTING
                    NodeStates.NODE_STATES_OFFLINE_UNSPECIFIED -> NodeState.OFFLINE
                    NodeStates.NODE_STATES_DRAINING -> NodeState.DRAINING
                    else -> NodeState.OFFLINE
                },
                definition.usedMemory,
                definition.cpuUsage,
                definition.onlinePlayers,
                definition.timestamp,
                definition.locked,
            )
        }
    }
}
