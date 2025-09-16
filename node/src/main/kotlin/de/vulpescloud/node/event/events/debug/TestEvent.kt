package de.vulpescloud.node.event.events.debug

import kotlinx.serialization.Serializable

@Serializable
data class TestEvent(
    val message: String,
    val sender: String,
    val time: Long = System.currentTimeMillis(),
    val id: Int
)
