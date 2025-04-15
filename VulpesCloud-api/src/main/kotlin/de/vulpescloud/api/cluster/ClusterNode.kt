package de.vulpescloud.api.cluster

import java.util.UUID

data class ClusterNode(
    val name: String,
    val uuid: UUID,
    var runningServices: Int,
    var state: NodeStates,
    var currentMemoryUsage: Int,
    var maxMemoryUsage: Int,
    val cloudVersion: String,
    val isHeadNode: Boolean,
    val hostname: String
)
