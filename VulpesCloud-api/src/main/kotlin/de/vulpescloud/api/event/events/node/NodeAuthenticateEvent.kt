package de.vulpescloud.api.event.events.node

import de.vulpescloud.api.event.Event

data class NodeAuthenticateEvent(
    val test: String // todo Add the Cluster stuff
) : Event {
    override fun name(): String {
        return "NodeAuthenticateEvent"
    }
}
