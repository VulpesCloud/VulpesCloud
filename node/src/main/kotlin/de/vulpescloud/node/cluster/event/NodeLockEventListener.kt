package de.vulpescloud.node.cluster.event

import build.buf.gen.vulpescloud.events.v1.NodeLockEvent
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.services.ServiceScheduler
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

object NodeLockEventListener {
    private var job: Job? = null
    private val logger = LoggerFactory.getLogger(NodeLockEventListener::class.java)

    fun subscribe() {
        job = EventsService.subscribe<NodeLockEvent> { event ->
            logger.info("Node <aqua>${event.node.name}</aqua> <gray> has been <red>locked</red>!</gray>")
            if (event.node.name == Node.instance.configProvider.config.nodeName) {
                logger.warn("This Node has been locked!")
                ServiceScheduler.stop()
                logger.warn("Stopping all remaining services on this node!")
                Node.instance.nodeServices.forEach { it.stop() }
                logger.warn("This Node will no longer manage services until it is unlocked!")
            }
        }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
