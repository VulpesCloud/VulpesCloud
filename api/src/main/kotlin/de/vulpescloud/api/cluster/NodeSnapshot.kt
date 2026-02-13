package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.node.v1.NodeSnapshot
import build.buf.gen.vulpescloud.node.v1.NodeStates
import build.buf.gen.vulpescloud.node.v1.nodeSnapshot
import de.vulpescloud.api.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import org.bson.*
import java.util.*

@Serializable
data class NodeSnapshot(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val state: NodeState,
    val usedMemory: Int,
    val cpuUsage: Double,
    val onlinePlayers: Int,
    val timestamp: Long,
) {

    fun toDefinition(): NodeSnapshot {
        return nodeSnapshot {
            this.name = this@NodeSnapshot.name
            this.uuid = this@NodeSnapshot.uuid.toString()
            this.usedMemory = this@NodeSnapshot.usedMemory
            this.onlinePlayers = this@NodeSnapshot.onlinePlayers
            this.timestamp = this@NodeSnapshot.timestamp
            this.state =
                when (this@NodeSnapshot.state) {
                    NodeState.DRAINING -> NodeStates.NODE_STATES_DRAINING
                    NodeState.OFFLINE -> NodeStates.NODE_STATES_OFFLINE_UNSPECIFIED
                    NodeState.ONLINE -> NodeStates.NODE_STATES_ONLINE
                    NodeState.BOOTING -> NodeStates.NODE_STATES_BOOTING
                }
        }
    }

    fun toDocument(): BsonDocument {
        return BsonDocument().apply {
            append("name", BsonString(name))
            append("uuid", BsonString(uuid.toString()))
            append("usedMemory", BsonInt32(usedMemory))
            append("onlinePlayers", BsonInt32(onlinePlayers))
            append("timestamp", BsonInt64(timestamp))
            append("state", BsonString(state.name))
            append("cpuUsage", BsonDouble(cpuUsage))
        }
    }

    companion object {
        fun fromDefinition(definition: NodeSnapshot): de.vulpescloud.api.cluster.NodeSnapshot {
            return de.vulpescloud.api.cluster.NodeSnapshot(
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
                definition.cpuUsage.toDouble(),
                definition.onlinePlayers,
                definition.timestamp,
            )
        }

        fun fromDocument(document: BsonDocument): de.vulpescloud.api.cluster.NodeSnapshot {
            return NodeSnapshot(
                document.getString("name").value,
                UUID.fromString(document.getString("uuid").value),
                when (document.getString("state").value) {
                    "ONLINE" -> NodeState.ONLINE
                    "BOOTING" -> NodeState.BOOTING
                    "OFFLINE" -> NodeState.OFFLINE
                    "DRAINING" -> NodeState.DRAINING
                    else -> NodeState.OFFLINE
                },
                document.getInt32("usedMemory").value,
                document.getDouble("cpuUsage").value,
                document.getInt32("onlinePlayers").value,
                document.getInt64("timestamp").value,
            )
        }
    }
}
