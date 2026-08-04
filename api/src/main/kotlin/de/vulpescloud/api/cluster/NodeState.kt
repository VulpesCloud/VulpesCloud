package de.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.cluster.v2.NodeState

enum class NodeState {

    OFFLINE,
    BOOTING,
    ONLINE,
    DRAINING;

    fun toNodeStates(): NodeState {
        return when (this) {
            OFFLINE -> NodeState.NODE_STATES_OFFLINE_UNSPECIFIED
            BOOTING -> NodeState.NODE_STATES_BOOTING
            ONLINE -> NodeState.NODE_STATES_ONLINE
            DRAINING -> NodeState.NODE_STATES_DRAINING
        }
    }
}
