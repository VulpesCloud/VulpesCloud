package de.vulpescloud.node.cluster.event

import build.buf.gen.vulpescloud.events.v1.NodeStateChangeEvent
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
                    "Node <aqua>${it.node.name}</aqua> is now <light_purple>${it.newState}</light_purple>"
                )
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
