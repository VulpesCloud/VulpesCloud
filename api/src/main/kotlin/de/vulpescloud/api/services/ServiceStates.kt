package de.vulpescloud.api.services

import build.buf.gen.vulpescloud.services.v1.ServiceState

enum class ServiceStates {
    UNKNOWN,
    RUNNING,
    STARTING,
    PREPARED,
    STOPPED;

    fun toServiceState(): ServiceState {
        return when (this) {
            UNKNOWN -> ServiceState.SERVICE_STATE_UNSPECIFIED
            RUNNING -> ServiceState.SERVICE_STATE_RUNNING
            STARTING -> ServiceState.SERVICE_STATE_STARTING
            PREPARED -> ServiceState.SERVICE_STATE_PREPARED
            STOPPED -> ServiceState.SERVICE_STATE_STOPPED
        }
    }
}

fun ServiceState.toServiceStates(): ServiceStates {
    return when (this) {
        ServiceState.SERVICE_STATE_UNSPECIFIED -> ServiceStates.UNKNOWN
        ServiceState.SERVICE_STATE_RUNNING -> ServiceStates.RUNNING
        ServiceState.SERVICE_STATE_STARTING -> ServiceStates.STARTING
        ServiceState.SERVICE_STATE_PREPARED -> ServiceStates.PREPARED
        ServiceState.SERVICE_STATE_STOPPED -> ServiceStates.STOPPED
        ServiceState.UNRECOGNIZED -> ServiceStates.UNKNOWN
    }
}
