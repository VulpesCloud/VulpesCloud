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

package org.vulpesstudios.vulpescloud.node.drain

import build.buf.gen.vulpescloud.cluster.v2.NodeDrainCancelledEvent
import build.buf.gen.vulpescloud.cluster.v2.NodeDrainCompletedEvent
import build.buf.gen.vulpescloud.cluster.v2.NodeDrainFailedEvent
import build.buf.gen.vulpescloud.cluster.v2.NodeDrainProgressEvent
import build.buf.gen.vulpescloud.cluster.v2.NodeDrainStartedEvent
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.event.EventsService

object DrainEventListener {
    private val drainLogger = LoggerFactory.getLogger("NodeDrain")
    private val jobs = mutableListOf<Job>()

    fun subscribe() {
        jobs += EventsService.subscribe<NodeDrainStartedEvent> { event ->
            val strategy = event.strategy.name.removePrefix("NODE_DRAIN_STRATEGY_")
            drainLogger.info(
                "Node <yellow>${event.snapshot.name}</yellow> <gold>started draining</gold> using strategy <white>$strategy</white>"
            )
        }
        jobs += EventsService.subscribe<NodeDrainProgressEvent> { event ->
            drainLogger.info(
                "Node <yellow>${event.snapshot.name}</yellow> <gray>drain progress:</gray> <white>${event.servicesRemaining}</white> <gray>service(s) and</gray> <white>${event.playersRemaining}</white> <gray>player(s) remaining</gray>"
            )
        }
        jobs += EventsService.subscribe<NodeDrainCompletedEvent> { event ->
            drainLogger.info("Node <yellow>${event.snapshot.name}</yellow> <green>completed its drain</green>")
        }
        jobs += EventsService.subscribe<NodeDrainCancelledEvent> { event ->
            drainLogger.warn("Node <yellow>${event.snapshot.name}</yellow> <red>drain was cancelled</red>")
        }
        jobs += EventsService.subscribe<NodeDrainFailedEvent> { event ->
            drainLogger.error(
                "Node <yellow>${event.snapshot.name}</yellow> <red>drain failed:</red> <white>${event.reason}</white>"
            )
        }
    }

    fun unsubscribe() {
        jobs.forEach(Job::cancel)
        jobs.clear()
    }
}
