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

package org.vulpesstudios.vulpescloud.node.cluster.event

import build.buf.gen.vulpescloud.cluster.v2.NodeStateChangeEvent
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.event.EventsService

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
