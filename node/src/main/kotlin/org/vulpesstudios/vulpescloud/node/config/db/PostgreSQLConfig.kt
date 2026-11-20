package org.vulpesstudios.vulpescloud.node.config.db

import kotlinx.serialization.Serializable

@Serializable
data class PostgreSQLConfig(
    val jdbcUrl: String = "jdbc:postgresql://localhost:5432/vulpescloud",
    val user: String = "vulpescloud",
    val password: String = "password",
)
