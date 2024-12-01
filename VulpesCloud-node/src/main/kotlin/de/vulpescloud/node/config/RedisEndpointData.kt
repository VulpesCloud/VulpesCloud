package de.vulpescloud.node.config

data class RedisEndpointData(
    val user: String,
    val hostname: String,
    val port: Int,
    val password: String
)
