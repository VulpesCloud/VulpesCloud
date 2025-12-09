package de.vulpescloud.node.config

import kotlinx.serialization.Serializable

@Serializable
data class AuthConfig(
    val jwtSecret: String,
    val jwtRefreshSecret: String,
    val allowAuthentication: Boolean,
)
