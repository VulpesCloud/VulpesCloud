package de.vulpescloud.api.cluster

import kotlinx.serialization.Serializable

@Serializable
data class ClusterConfig (
    val nodes: List<NodeEndpointDetails>,
    val ipWhitelist: List<String>,
)
