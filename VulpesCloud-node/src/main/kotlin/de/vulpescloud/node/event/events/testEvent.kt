package de.vulpescloud.node.event.events

import de.vulpescloud.api.event.Event

object testEvent: Event {

    val text = "Hewwow"

    fun trigger() {
        println("hehe")
    }

    fun name(): String {
        return "testEvent"
    }
}