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

package org.vulpesstudios.vulpescloud.api.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateJobStatus as ProtoTemplateJobStatus

enum class TemplateJobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    FAILED;

    val isActive: Boolean
        get() = this == PENDING || this == IN_PROGRESS

    val isFinished: Boolean
        get() = !isActive

    fun toDefinition(): ProtoTemplateJobStatus =
        when (this) {
            PENDING -> ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_PENDING
            IN_PROGRESS -> ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_IN_PROGRESS
            COMPLETED -> ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_COMPLETED
            CANCELLED -> ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_CANCELLED
            FAILED -> ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_FAILED
        }

    companion object {
        fun fromDefinition(proto: ProtoTemplateJobStatus): TemplateJobStatus =
            when (proto) {
                ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_IN_PROGRESS -> IN_PROGRESS
                ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_COMPLETED -> COMPLETED
                ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_CANCELLED -> CANCELLED
                ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_FAILED -> FAILED
                ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_PENDING,
                ProtoTemplateJobStatus.TEMPLATE_JOB_STATUS_UNSPECIFIED,
                ProtoTemplateJobStatus.UNRECOGNIZED -> PENDING
            }
    }
}
