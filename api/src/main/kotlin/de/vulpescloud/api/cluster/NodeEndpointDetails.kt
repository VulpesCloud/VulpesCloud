package de.vulpescloud.api.cluster

import de.vulpescloud.api.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class NodeEndpointDetails(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val host: String,
    val port: Int,
)
