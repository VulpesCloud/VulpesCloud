package de.vulpescloud.api.events.cluster

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeState
import kotlinx.serialization.Serializable

@Serializable
data class NodeStateChangeEvent(
    val node: ClusterNode,
    val oldState: NodeState,
    val newState: NodeState,
)
