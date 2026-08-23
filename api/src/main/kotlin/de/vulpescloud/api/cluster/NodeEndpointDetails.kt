package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.cluster.v2.Node
import build.buf.gen.vulpescloud.cluster.v2.node
import de.vulpescloud.api.serializer.UUIDSerializer
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class NodeEndpointDetails(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val host: String,
    val port: Int,
) {
    fun toDefinition(): Node {
        return node {
            this.name = this@NodeEndpointDetails.name
            this.uuid = this@NodeEndpointDetails.uuid.toString()
            this.grpcAddress = this@NodeEndpointDetails.host
            this.grpcPort = this@NodeEndpointDetails.port
        }
    }

    companion object {
        fun fromDefinition(definition: Node): NodeEndpointDetails {
            return NodeEndpointDetails(
                definition.name,
                UUID.fromString(definition.uuid),
                definition.grpcAddress,
                definition.grpcPort,
            )
        }
    }
}
