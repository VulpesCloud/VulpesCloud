package de.vulpescloud.api.event.events.service

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.service.ServiceInfo
import de.vulpescloud.api.service.ServiceStates

data class ServiceStateChangeEvent(
    val serviceInfo: ServiceInfo,
    val oldState: ServiceStates,
    val newState: ServiceStates,
) : Event
