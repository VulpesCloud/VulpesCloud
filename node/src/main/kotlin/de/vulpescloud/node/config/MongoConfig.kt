package de.vulpescloud.node.config

import kotlinx.serialization.Serializable

@Serializable
data class MongoConfig(
    val connectionString: String,
    val connectionPrefix: String
)
