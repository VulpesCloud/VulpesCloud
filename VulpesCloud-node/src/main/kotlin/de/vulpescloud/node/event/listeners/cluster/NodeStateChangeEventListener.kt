package de.vulpescloud.node.event.listeners.cluster

import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.cluster.NodeStateChangeEvent
import de.vulpescloud.api.lang.Translator
import org.slf4j.LoggerFactory

class NodeStateChangeEventListener(private val translator: Translator) {

    private val logger = LoggerFactory.getLogger(NodeStateChangeEventListener::class.java)

    @EventListener
    fun handeNodeStateChangeEvent(event: NodeStateChangeEvent) {
        logger.info(
            translator.trans("EVENTS.NodeStateChangeEvent.MESSAGE"),
            event.node.name,
            event.newState,
        )
    }
}
