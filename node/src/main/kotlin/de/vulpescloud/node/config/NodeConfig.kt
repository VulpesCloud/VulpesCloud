package de.vulpescloud.node.config

import de.vulpescloud.node.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class NodeConfig(
    val nodeName: String,
    @Serializable(with = UUIDSerializer::class) val uuid: UUID,
    val grpcPort: Int,
    val grpcHost: String,
    val hostname: String,
    val mongodb: MongoConfig,
)
