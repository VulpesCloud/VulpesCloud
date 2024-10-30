package io.github.thecguygithub.node.networking.mysql

data class MySQLEndpointData(
    val user: String,
    val password: String,
    val database: String,
    val host: String,
    val port: Int,
    val ssl: Boolean
)
