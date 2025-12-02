package de.vulpescloud.node.event

import de.vulpescloud.node.cluster.event.NodeStateChangeEventListener
import de.vulpescloud.node.services.ServiceLogHandler
import de.vulpescloud.node.services.impl.ServiceStateChangeEventListener

object EventListenHelper {

    fun subscribeToEvents() {
        ServiceLogHandler.subscribe()
        NodeStateChangeEventListener.subscribe()
        ServiceStateChangeEventListener.subscribe()
    }

    fun unsubscribeFromEvents() {
        NodeStateChangeEventListener.unsubscribe()
        ServiceStateChangeEventListener.unsubscribe()
    }
}
