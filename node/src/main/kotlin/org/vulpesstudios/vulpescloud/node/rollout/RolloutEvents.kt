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
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.event.EventsService

object RolloutEvents {

    fun started(progress: RolloutProgress, task: Task) {
        EventsService.publish(
            rolloutStartedEvent {
                this.progress = progress.toDefinition()
                this.task = task.toDefinition()
            },
            true,
        )
    }

    fun batchProgress(
        progress: RolloutProgress,
        task: Task,
        newServices: List<Service> = emptyList(),
        oldServices: List<Service> = emptyList(),
    ) {
        EventsService.publish(
            rolloutBatchProgressEvent {
                this.rolloutId = progress.rolloutId
                this.task = task.toDefinition()
                this.newServices.addAll(newServices.map { it.toDefinition() })
                this.oldServices.addAll(oldServices.map { it.toDefinition() })
                this.servicesStarted = progress.servicesStarted
                this.servicesReady = progress.servicesReady
                this.servicesStopped = progress.servicesStopped
                this.totalServicesToReplace = progress.totalServicesToReplace
            },
            true,
        )
    }

    fun draining(progress: RolloutProgress, task: Task, drainingServices: List<Service>) {
        EventsService.publish(
            rolloutDrainingEvent {
                this.rolloutId = progress.rolloutId
                this.task = task.toDefinition()
                this.drainingServices.addAll(drainingServices.map { it.toDefinition() })
                progress.options.drainPlayerThreshold?.let { this.drainPlayerThreshold = it }
                progress.options.drainTimeout?.let { this.drainTimeout = it }
            },
            true,
        )
    }

    fun completed(progress: RolloutProgress, task: Task) {
        EventsService.publish(
            rolloutCompletedEvent {
                this.progress = progress.toDefinition()
                this.task = task.toDefinition()
            },
            true,
        )
    }

    fun failed(progress: RolloutProgress, task: Task) {
        EventsService.publish(
            rolloutFailedEvent {
                this.progress = progress.toDefinition()
                this.task = task.toDefinition()
                this.failureReason = progress.failureReason
            },
            true,
        )
    }

    fun cancelled(progress: RolloutProgress, task: Task, terminatedServices: List<Service> = emptyList()) {
        EventsService.publish(
            rolloutCancelledEvent {
                this.progress = progress.toDefinition()
                this.task = task.toDefinition()
                this.terminatedServices.addAll(terminatedServices.map { it.toDefinition() })
            },
            true,
        )
    }
}
