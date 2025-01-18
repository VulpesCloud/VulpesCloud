package de.vulpescloud.api.event.events.service

import de.vulpescloud.api.services.Service

data class ServiceDeleteEvent(
    val service: Service
)
