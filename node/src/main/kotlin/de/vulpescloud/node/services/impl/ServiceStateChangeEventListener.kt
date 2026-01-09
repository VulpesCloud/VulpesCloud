package de.vulpescloud.node.services.impl

import de.vulpescloud.api.events.services.ServiceStateChangeEvent
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.utils.MongoUtils
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

object ServiceStateChangeEventListener {

    private var job: Job? = null
    private val logger = LoggerFactory.getLogger("ServiceStateChangeEventListener")

    fun subscribe() {
        job =
            EventsService.subscribe<ServiceStateChangeEvent> {
                logger.info(
                    "Service <aqua>${it.event.service.task.name}-${it.event.service.orderedId}</aqua> is now <light_purple>${it.event.newState}</light_purple>"
                )

                if (it.event.newState == ServiceStates.RUNNING) {
                    MongoUtils.updateService(it.event.service.copy(state = it.event.newState))
                }
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
