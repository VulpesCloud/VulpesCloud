package de.vulpescloud.node.event

import de.vulpescloud.node.cluster.event.NodeStateChangeEventListener
import de.vulpescloud.node.services.ServiceLogHandler

object EventListenHelper {

    fun subscribeToEvents() {
        ServiceLogHandler.subscribe()
        NodeStateChangeEventListener.subscribe()
    }

    fun unsubscribeFromEvents() {
        NodeStateChangeEventListener.unsubscribe()
    }
}
