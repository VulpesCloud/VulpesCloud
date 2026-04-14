package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.node.v1.NodeStates

enum class NodeState {

    OFFLINE,
    BOOTING,
    ONLINE,
    DRAINING;

    fun toNodeStates(): NodeStates {
        return when (this) {
            OFFLINE -> NodeStates.NODE_STATES_OFFLINE_UNSPECIFIED
            BOOTING -> NodeStates.NODE_STATES_BOOTING
            ONLINE -> NodeStates.NODE_STATES_ONLINE
            DRAINING -> NodeStates.NODE_STATES_DRAINING
        }
    }
}
