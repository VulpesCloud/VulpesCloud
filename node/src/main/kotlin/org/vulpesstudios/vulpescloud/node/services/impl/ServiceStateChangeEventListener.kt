/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.services.impl

import build.buf.gen.vulpescloud.events.v1.ServiceStateChangedEvent
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.services.ServiceStates
import org.vulpesstudios.vulpescloud.api.services.toServiceStates
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.event.EventsService

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
                    val abstractService =
                        Node.instance.nodeServices.find { service ->
                            service.service.uuid.toString() == it.service.uuid
                        }
                    if (abstractService != null) {
                        abstractService.service =
                            abstractService.service.copy(state = it.newState.toServiceStates())
                    }
                }
            }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
