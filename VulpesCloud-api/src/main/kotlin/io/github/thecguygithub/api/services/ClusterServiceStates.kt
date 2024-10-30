package io.github.thecguygithub.api.services

enum class ClusterServiceStates {

    PREPARED,
    CONNECTING,
    STARTING,
    ONLINE,
    STOPPING,
    LOST;

}