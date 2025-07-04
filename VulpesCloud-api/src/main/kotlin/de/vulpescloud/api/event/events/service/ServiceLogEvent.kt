package de.vulpescloud.api.event.events.service

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.service.ServiceInfo

data class ServiceLogEvent(val serviceInfo: ServiceInfo, val rawMessage: String) : Event
