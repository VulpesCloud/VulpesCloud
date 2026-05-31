package de.vulpescloud.node.event

import de.vulpescloud.node.cluster.event.NodeStateChangeEventListener
import de.vulpescloud.node.players.PlayerJoinEventListener
import de.vulpescloud.node.players.PlayerQuitEventListener
import de.vulpescloud.node.players.PlayerSwitchServerEventListener
import de.vulpescloud.node.services.ServiceLogHandler
import de.vulpescloud.node.services.impl.ServiceStateChangeEventListener

object EventListenHelper {

    fun subscribeToEvents() {
        ServiceLogHandler.subscribe()
        NodeStateChangeEventListener.subscribe()
        ServiceStateChangeEventListener.subscribe()

        PlayerQuitEventListener.subscribe()
        PlayerJoinEventListener.subscribe()
        PlayerSwitchServerEventListener.subscribe()
    }

    fun unsubscribeFromEvents() {
        NodeStateChangeEventListener.unsubscribe()
        ServiceStateChangeEventListener.unsubscribe()

        PlayerQuitEventListener.unsubscribe()
        PlayerJoinEventListener.unsubscribe()
        PlayerSwitchServerEventListener.unsubscribe()
    }
}
