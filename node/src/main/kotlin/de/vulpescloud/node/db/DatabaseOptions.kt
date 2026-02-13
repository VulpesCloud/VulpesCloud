package de.vulpescloud.node.db

data class DatabaseOptions(
    val user: String,
    val password: String,
    val host: String,
    val port: Int,
    val database: String,
    val connectionString: String
)
