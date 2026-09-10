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

package org.vulpesstudios.vulpescloud.node.rollout

import build.buf.gen.vulpescloud.events.v1.*
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.event.EventsService

object RolloutEventListener {

    private val logger = LoggerFactory.getLogger("Rollout")
    private val jobs = mutableListOf<Job>()

    fun subscribe() {
        jobs +=
            EventsService.subscribe<RolloutStartedEvent> {
                val strategy = it.progress.strategyName()
                logger.info(
                    "Rollout <yellow>${it.progress.rolloutId}</yellow> <gray>started for task</gray> <aqua>${it.task.name}</aqua> <gray>using strategy</gray> <white>$strategy</white> <gray>(</gray><white>${it.progress.totalServicesToReplace}</white> <gray>service(s) to replace)</gray>"
                )
            }

        jobs +=
            EventsService.subscribe<RolloutBatchProgressEvent> {
                logger.info(
                    "Rollout <yellow>${it.rolloutId}</yellow> <gray>(</gray><aqua>${it.task.name}</aqua><gray>): batch done -</gray> <green>${it.servicesReady}</green><gray>/</gray><white>${it.totalServicesToReplace}</white> <gray>ready,</gray> <red>${it.servicesStopped}</red><gray>/</gray><white>${it.totalServicesToReplace}</white> <gray>replaced</gray>"
                )
            }

        jobs +=
            EventsService.subscribe<RolloutDrainingEvent> {
                val threshold = if (it.hasDrainPlayerThreshold()) it.drainPlayerThreshold.toString() else "0"
                val timeout =
                    if (it.hasDrainTimeout() && it.drainTimeout.seconds > 0) "${it.drainTimeout.seconds}s"
                    else "indefinite"
                logger.info(
                    "Rollout <yellow>${it.rolloutId}</yellow> <gray>(</gray><aqua>${it.task.name}</aqua><gray>):</gray> <white>${it.drainingServicesCount}</white> <gray>service(s) now</gray> <gold>draining</gold> <gray>(threshold=</gray><white>$threshold</white><gray>, timeout=</gray><white>$timeout</white><gray>)</gray>"
                )
            }

        jobs +=
            EventsService.subscribe<RolloutCompletedEvent> {
                logger.info(
                    "Rollout <yellow>${it.progress.rolloutId}</yellow> <gray>for task</gray> <aqua>${it.task.name}</aqua> <green>completed</green> <gray>successfully (</gray><white>${it.progress.servicesStopped}</white> <gray>service(s) replaced)</gray>"
                )
            }

        jobs +=
            EventsService.subscribe<RolloutFailedEvent> {
                logger.error(
                    "Rollout <yellow>${it.progress.rolloutId}</yellow> <gray>for task</gray> <aqua>${it.task.name}</aqua> <red>failed</red><gray>:</gray> <white>${it.failureReason}</white>"
                )
            }

        jobs +=
            EventsService.subscribe<RolloutCancelledEvent> {
                logger.warn(
                    "Rollout <yellow>${it.progress.rolloutId}</yellow> <gray>for task</gray> <aqua>${it.task.name}</aqua> <red>was cancelled</red> <gray>(</gray><white>${it.terminatedServicesCount}</white> <gray>new service(s) terminated)</gray>"
                )
            }
    }

    fun unsubscribe() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    private fun build.buf.gen.vulpescloud.rollout.v1.RolloutProgress.strategyName(): String =
        options.strategy.name.removePrefix("ROLLOUT_STRATEGY_")
}
