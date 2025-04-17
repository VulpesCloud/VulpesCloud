package de.vulpescloud.api.event.events.cluster

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.event.Event
import kotlinx.serialization.Serializable

@Serializable
data class NodeStateChangeEvent(
    val node: ClusterNode,
    val oldState: NodeStates,
    val newState: NodeStates
) : Event
