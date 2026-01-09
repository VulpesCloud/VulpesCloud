package de.vulpescloud.api.events.services

import de.vulpescloud.api.services.Service
import kotlinx.serialization.Serializable

@Serializable
data class ServiceLogEvent(
    val service: Service,
    val message: String
)
