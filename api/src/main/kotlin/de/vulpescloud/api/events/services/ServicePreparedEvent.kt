package de.vulpescloud.api.events.services

import de.vulpescloud.api.services.Service
import kotlinx.serialization.Serializable

@Serializable data class ServicePreparedEvent(val service: Service)
