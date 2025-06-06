package de.vulpescloud.api.event.events.service

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.service.Service

data class ServiceLogEvent(val service: Service, val rawMessage: String) : Event
