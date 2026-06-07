package de.vulpescloud.node.cluster.event

import build.buf.gen.vulpescloud.events.v1.NodeUnlockEvent
import de.vulpescloud.node.event.EventsService
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

object NodeUnlockEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(NodeUnlockEventListener::class.java)

    fun subscribe() {
        job = EventsService.subscribe<NodeUnlockEvent> { event ->
            logger.info("Node <aqua>${event.node.name}</aqua> <gray> has been <green>unlocked</green>!")
        }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
