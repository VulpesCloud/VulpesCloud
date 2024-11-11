package de.vulpescloud.api.services

enum class ClusterServiceStates {

    PREPARED,
    CONNECTING,
    STARTING,
    ONLINE,
    STOPPING,
    LOST;

}