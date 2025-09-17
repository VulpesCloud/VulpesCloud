package de.vulpescloud.api.events

import java.util.UUID

data class Event<T>(
    val id: UUID,
    val type: String,
    val metadata: Map<String, String>,
    val event: T,
    val timestamp: Long
)
