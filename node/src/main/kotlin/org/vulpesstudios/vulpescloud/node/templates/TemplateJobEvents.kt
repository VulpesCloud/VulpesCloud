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
import build.buf.gen.vulpescloud.templates.v1.TemplateJobType as ProtoTemplateJobType
import kotlinx.coroutines.runBlocking
import org.vulpesstudios.vulpescloud.api.templates.TemplateJobProgress
import org.vulpesstudios.vulpescloud.api.templates.TemplateJobType
import org.vulpesstudios.vulpescloud.node.event.EventsService

object TemplateJobEvents {

    fun started(progress: TemplateJobProgress) {
        runBlocking {
            EventsService.publish(
                templateJobStartedEvent {
                    this.jobId = progress.jobId
                    this.type = progress.type.toProtoType()
                    this.sourceName = progress.source.name
                    this.destinationName = progress.destination.name
                },
                true,
            )
        }
    }

    fun completed(progress: TemplateJobProgress) {
        runBlocking {
            EventsService.publish(
                templateJobCompletedEvent {
                    this.jobId = progress.jobId
                    this.type = progress.type.toProtoType()
                    this.sourceName = progress.source.name
                    this.destinationName = progress.destination.name
                    this.filesCopied = progress.filesCopied
                    this.filesSkipped = progress.filesSkipped
                    this.filesOverwritten = progress.filesOverwritten
                    this.filesPurged = progress.filesPurged
                },
                true,
            )
        }
    }

    fun failed(progress: TemplateJobProgress) {
        runBlocking {
            EventsService.publish(
                templateJobFailedEvent {
                    this.jobId = progress.jobId
                    this.type = progress.type.toProtoType()
                    this.sourceName = progress.source.name
                    this.destinationName = progress.destination.name
                    this.reason = progress.failureReason ?: "Unknown error"
                },
                true,
            )
        }
    }

    fun cancelled(progress: TemplateJobProgress) {
        runBlocking {
            EventsService.publish(
                templateJobCancelledEvent {
                    this.jobId = progress.jobId
                    this.type = progress.type.toProtoType()
                    this.sourceName = progress.source.name
                    this.destinationName = progress.destination.name
                },
                true,
            )
        }
    }

    private fun TemplateJobType.toProtoType(): ProtoTemplateJobType =
        when (this) {
            TemplateJobType.COPY -> ProtoTemplateJobType.TEMPLATE_JOB_TYPE_COPY
            TemplateJobType.MOVE -> ProtoTemplateJobType.TEMPLATE_JOB_TYPE_MOVE
        }
}
