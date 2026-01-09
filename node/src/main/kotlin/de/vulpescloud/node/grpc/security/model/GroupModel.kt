package de.vulpescloud.node.grpc.security.model

import kotlinx.serialization.Serializable

@Serializable data class GroupModel(val name: String, val permissions: List<String> = emptyList())
