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

import build.buf.gen.vulpescloud.templates.v1.TemplateJobType as ProtoTemplateJobType

enum class TemplateJobType {
    COPY,
    MOVE;

    fun toDefinition(): ProtoTemplateJobType =
        when (this) {
            COPY -> ProtoTemplateJobType.TEMPLATE_JOB_TYPE_COPY
            MOVE -> ProtoTemplateJobType.TEMPLATE_JOB_TYPE_MOVE
        }

    companion object {
        fun fromDefinition(proto: ProtoTemplateJobType): TemplateJobType =
            when (proto) {
                ProtoTemplateJobType.TEMPLATE_JOB_TYPE_COPY -> COPY
                ProtoTemplateJobType.TEMPLATE_JOB_TYPE_MOVE -> MOVE
                ProtoTemplateJobType.TEMPLATE_JOB_TYPE_UNSPECIFIED,
                ProtoTemplateJobType.UNRECOGNIZED -> COPY
            }
    }
}
