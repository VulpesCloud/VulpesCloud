package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.cluster.v2.NodeSnapshot
import build.buf.gen.vulpescloud.cluster.v2.NodeState
import build.buf.gen.vulpescloud.cluster.v2.nodeSnapshot
import de.vulpescloud.api.serializer.UUIDSerializer
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class NodeSnapshot(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val state: de.vulpescloud.api.cluster.NodeState,
    val nodeProcessUsedMemory: Long,
    val servicesUsedMemory: Long,
    val servicesMaxMemory: Long,
    val systemCpuUsage: Double,
    val onlinePlayers: Long,
    val timestamp: Long,
    val attributes: Map<String, String>,
    val services: Long,
) {

    fun toDefinition(): NodeSnapshot {
        return nodeSnapshot {
            this.name = this@NodeSnapshot.name
            this.uuid = this@NodeSnapshot.uuid.toString()
            this.servicesUsedMemory = this@NodeSnapshot.servicesUsedMemory
            this.onlinePlayers = this@NodeSnapshot.onlinePlayers
            this.timestamp = this@NodeSnapshot.timestamp
            this.systemCpuUsage = this@NodeSnapshot.systemCpuUsage
            this.nodeProccessUsedMemory = this@NodeSnapshot.nodeProcessUsedMemory
            this.servicesMaxMemory = this@NodeSnapshot.servicesMaxMemory
            this.attributes.putAll(attributes)
            this.services = this@NodeSnapshot.services
            this.state =
                when (this@NodeSnapshot.state) {
                    de.vulpescloud.api.cluster.NodeState.DRAINING -> NodeState.NODE_STATES_DRAINING
                    de.vulpescloud.api.cluster.NodeState.OFFLINE ->
                        NodeState.NODE_STATES_OFFLINE_UNSPECIFIED
                    de.vulpescloud.api.cluster.NodeState.ONLINE -> NodeState.NODE_STATES_ONLINE
                    de.vulpescloud.api.cluster.NodeState.BOOTING -> NodeState.NODE_STATES_BOOTING
                }
        }
    }

    companion object {
        fun fromDefinition(definition: NodeSnapshot): de.vulpescloud.api.cluster.NodeSnapshot {
            return NodeSnapshot(
                definition.name,
                UUID.fromString(definition.uuid),
                when (definition.state) {
                    NodeState.NODE_STATES_ONLINE -> de.vulpescloud.api.cluster.NodeState.ONLINE
                    NodeState.NODE_STATES_BOOTING -> de.vulpescloud.api.cluster.NodeState.BOOTING
                    NodeState.NODE_STATES_OFFLINE_UNSPECIFIED ->
                        de.vulpescloud.api.cluster.NodeState.OFFLINE
                    NodeState.NODE_STATES_DRAINING -> de.vulpescloud.api.cluster.NodeState.DRAINING
                    else -> de.vulpescloud.api.cluster.NodeState.OFFLINE
                },
                definition.nodeProccessUsedMemory,
                definition.servicesMaxMemory,
                definition.servicesUsedMemory,
                definition.systemCpuUsage,
                definition.onlinePlayers,
                definition.timestamp,
                definition.attributesMap,
                definition.services,
            )
        }
    }
}
