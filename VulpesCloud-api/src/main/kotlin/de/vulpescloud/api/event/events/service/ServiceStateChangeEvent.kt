package de.vulpescloud.api.event.events.service

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates

data class ServiceStateChangeEvent(
    val service: Service,
    val oldState: ServiceStates,
    val newState: ServiceStates
) : Event {
    override fun name(): String {
        return "ServiceStateChangeEvent"
    }
}
