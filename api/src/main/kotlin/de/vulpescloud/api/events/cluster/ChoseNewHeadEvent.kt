package de.vulpescloud.api.events.cluster

import de.vulpescloud.api.cluster.ClusterNode
import kotlinx.serialization.Serializable

@Serializable data class ChoseNewHeadEvent(val newHead: ClusterNode)
