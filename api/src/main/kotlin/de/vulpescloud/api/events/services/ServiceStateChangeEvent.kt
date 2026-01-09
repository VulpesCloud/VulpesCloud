package de.vulpescloud.api.events.services

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import kotlinx.serialization.Serializable

@Serializable
data class ServiceStateChangeEvent(
    val service: Service,
    val oldState: ServiceStates,
    val newState: ServiceStates
)
