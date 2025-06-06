package de.vulpescloud.api.event.events.service

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceStates

data class ServiceStateChangeEvent(
    val service: Service,
    val oldState: ServiceStates,
    val newState: ServiceStates,
) : Event
