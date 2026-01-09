package de.vulpescloud.node.config

import de.vulpescloud.api.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class NodeConfig(
    val nodeName: String,
    @Serializable(with = UUIDSerializer::class) val uuid: UUID,
    val grpcPort: Int,
    val grpcHost: String,
    val serviceBindAdress: String,
    val mongodb: MongoConfig,
    val maxMemory: Int = 1024, // MB
    val defaultServiceType: String = "LOCAL", // can be "LOCAL" or "DOCKER"
    val docker: DockerConfig,
    val useModernForwarding: Boolean,
    val auth: AuthConfig = AuthConfig("changeme", "changeme2", true),
)
