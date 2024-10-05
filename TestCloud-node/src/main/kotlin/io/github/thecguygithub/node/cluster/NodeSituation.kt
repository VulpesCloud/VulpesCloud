package io.github.thecguygithub.node.cluster



enum class NodeSituation {

    INITIALIZING,
    SYNC,
    RUNNING,
    STOPPING,
    DISCONNECTED,
    LOST,
    STOPPED;

    fun isStopping(): Boolean {
        return this == STOPPED || this == STOPPING
    }
}