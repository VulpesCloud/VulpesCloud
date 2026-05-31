package de.vulpescloud.node.grpc.security.model

import build.buf.gen.vulpescloud.auth.v1.ProtoUser
import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    val name: String,
    val password: String,
    val groups: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val extraData: Map<String, String> = emptyMap()
)