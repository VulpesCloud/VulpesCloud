package de.vulpescloud.node.event.listeners.service

import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import org.slf4j.LoggerFactory

class ServiceStateChangeEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onServiceStateChangeEvent(event: ServiceStateChangeEvent) {
        logger.info("Service ${event.service.name} is now ${event.newState}")
    }

}
