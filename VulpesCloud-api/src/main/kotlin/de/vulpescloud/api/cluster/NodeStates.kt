package de.vulpescloud.api.cluster

enum class NodeStates {

    OFFLINE,
    BOOTING,
    ONLINE,
    DRAINING,
    LOST,
    STOPPING;

}
