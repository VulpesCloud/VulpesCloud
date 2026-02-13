package de.vulpescloud.node.config.db

import kotlinx.serialization.Serializable

@Serializable
data class MariaDBConfig(
    val user: String,
    val password: String,
    val host: String,
    val port: Int,
    val database: String
)
