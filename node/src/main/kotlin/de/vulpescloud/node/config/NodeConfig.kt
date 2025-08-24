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
    val mongodb: MongoConfig,
    val maxMemory: Int = 1024, // MB
    val serviceType: String = "LOCAL" // can be "LOCAL" or "DOCKER"
)
