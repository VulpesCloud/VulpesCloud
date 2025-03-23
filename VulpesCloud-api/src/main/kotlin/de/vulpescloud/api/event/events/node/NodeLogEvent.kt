package de.vulpescloud.api.event.events.node

import de.vulpescloud.api.event.Event

data class NodeLogEvent(
    val line: String,
    val level: String,
    val thread: String
): Event {
    override fun name(): String {
        return "NodeLogEvent"
    }
}