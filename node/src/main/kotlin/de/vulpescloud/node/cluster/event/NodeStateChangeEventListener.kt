package de.vulpescloud.node.cluster.event

import build.buf.gen.vulpescloud.cluster.v2.NodeStateChangeEvent
import de.vulpescloud.node.event.EventsService
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

object NodeStateChangeEventListener {

    private var job: Job? = null
    private val logger = LoggerFactory.getLogger("NodeStateChangeEventListener")

    fun subscribe() {
        job =
            EventsService.subscribe<NodeStateChangeEvent> {
                logger.info(
                    "Node <aqua>${it.snapshot.name}</aqua> <gray>changed state to</gray> <white>${it.newState}</white>"
                )
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
