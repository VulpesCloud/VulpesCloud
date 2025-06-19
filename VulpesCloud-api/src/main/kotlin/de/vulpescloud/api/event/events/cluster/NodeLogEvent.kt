package de.vulpescloud.api.event.events.cluster

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.event.Event

data class NodeLogEvent(
    val node: ClusterNode,
    val level: String,
    val log: String,
    val formattedLog: String
) : Event
