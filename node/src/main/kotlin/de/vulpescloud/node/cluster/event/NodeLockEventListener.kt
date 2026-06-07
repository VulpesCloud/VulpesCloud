package de.vulpescloud.node.cluster.event

import build.buf.gen.vulpescloud.events.v1.NodeLockEvent
import de.vulpescloud.node.event.EventsService
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

object NodeLockEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(NodeLockEventListener::class.java)

    fun subscribe() {
        job = EventsService.subscribe<NodeLockEvent> { event ->
            logger.info("Node <aqua>${event.node.name}</aqua> <gray> has been <red>locked</red>!</gray>")
        }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
