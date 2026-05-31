package de.vulpescloud.node.services.impl

import build.buf.gen.vulpescloud.events.v1.ServiceStateChangedEvent
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.services.toServiceStates
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.utils.MongoUtils
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

object ServiceStateChangeEventListener {

    private var job: Job? = null
    private val logger = LoggerFactory.getLogger("ServiceStateChangeEventListener")

    fun subscribe() {
        job =
            EventsService.subscribe<ServiceStateChangedEvent> {
                logger.info(
                    "Service <aqua>${it.service.task.name}-${it.service.orderedId}</aqua> <gray>is now</gray> <white>${it.newState}</white> <gray>on node</gray> <white>${it.service.node}</white>"
                )

                if (it.newState == ServiceStates.RUNNING.toServiceState()) {
                    MongoUtils.updateService(
                        Service.fromDefinition(it.service)
                            .copy(state = it.newState.toServiceStates())
                    )
                }
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
