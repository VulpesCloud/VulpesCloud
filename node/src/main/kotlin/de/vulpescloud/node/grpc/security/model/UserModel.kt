package de.vulpescloud.node.grpc.security.model

import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    val name: String,
    val password: String,
    val groups: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
)
