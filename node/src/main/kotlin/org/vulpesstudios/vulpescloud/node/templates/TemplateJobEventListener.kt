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

package org.vulpesstudios.vulpescloud.node.templates

import build.buf.gen.vulpescloud.events.v1.*
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.event.EventsService

object TemplateJobEventListener {
    private val jobLogger = LoggerFactory.getLogger("TemplateJob")
    private val jobs = mutableListOf<Job>()

    fun subscribe() {
        jobs += EventsService.subscribe<TemplateJobStartedEvent> { event ->
            val type = event.type.name.removePrefix("TEMPLATE_JOB_TYPE_")
            jobLogger.info(
                "Template <gold>${event.sourceName}</gold> <gray>$type job started</gray> " +
                    "<dark_gray>(</dark_gray><white>${event.jobId}</white><dark_gray>)</dark_gray> " +
                    "<gray>→</gray> <gold>${event.destinationName}</gold>"
            )
        }
        jobs += EventsService.subscribe<TemplateJobCompletedEvent> { event ->
            val type = event.type.name.removePrefix("TEMPLATE_JOB_TYPE_")
            jobLogger.info(
                "Template <gold>${event.sourceName}</gold> <green>$type job completed</green> " +
                    "<dark_gray>(</dark_gray><white>${event.jobId}</white><dark_gray>)</dark_gray> " +
                    "<gray>| Copied:</gray> <white>${event.filesCopied}</white> " +
                    "<gray>| Skipped:</gray> <white>${event.filesSkipped}</white> " +
                    "<gray>| Overwritten:</gray> <white>${event.filesOverwritten}</white> " +
                    "<gray>| Purged:</gray> <white>${event.filesPurged}</white>"
            )
        }
        jobs += EventsService.subscribe<TemplateJobFailedEvent> { event ->
            val type = event.type.name.removePrefix("TEMPLATE_JOB_TYPE_")
            jobLogger.error(
                "Template <gold>${event.sourceName}</gold> <red>$type job failed</red> " +
                    "<dark_gray>(</dark_gray><white>${event.jobId}</white><dark_gray>)</dark_gray>: " +
                    "<white>${event.reason}</white>"
            )
        }
        jobs += EventsService.subscribe<TemplateJobCancelledEvent> { event ->
            val type = event.type.name.removePrefix("TEMPLATE_JOB_TYPE_")
            jobLogger.warn(
                "Template <gold>${event.sourceName}</gold> <red>$type job cancelled</red> " +
                    "<dark_gray>(</dark_gray><white>${event.jobId}</white><dark_gray>)</dark_gray>"
            )
        }
    }

    fun unsubscribe() {
        jobs.forEach(Job::cancel)
        jobs.clear()
    }
}
