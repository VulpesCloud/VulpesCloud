package de.vulpescloud.node.event.listeners.service

import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.service.ServiceLogEvent
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.node.service.ServiceProviderImpl
import org.slf4j.LoggerFactory

class ServiceLogEventListener(
    serviceProvider: ServiceProvider
) {
    private val serviceProvider = serviceProvider as ServiceProviderImpl
    private val logger = LoggerFactory.getLogger(ServiceLogEventListener::class.java)

    @EventListener
    fun handleServiceLogEvent(event: ServiceLogEvent) {
        if (serviceProvider.loggingServices.contains(event.serviceInfo.name)) {
            logger.info("&8[ &m{} &8] &b{}", event.serviceInfo.name, event.rawMessage)
        }
    }
}
