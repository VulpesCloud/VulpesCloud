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

import build.buf.gen.vulpescloud.templates.v1.TemplateJobConflictMode as ProtoTemplateJobConflictMode

enum class TemplateJobConflictMode {
    DENY,
    SKIP,
    OVERWRITE,
    OVERWRITE_NEWER,
    OVERWRITE_CHANGED;

    fun toDefinition(): ProtoTemplateJobConflictMode =
        when (this) {
            DENY -> ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_DENY
            SKIP -> ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_SKIP
            OVERWRITE -> ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE
            OVERWRITE_NEWER -> ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE_NEWER
            OVERWRITE_CHANGED -> ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE_CHANGED
        }

    companion object {
        fun fromDefinition(proto: ProtoTemplateJobConflictMode): TemplateJobConflictMode =
            when (proto) {
                ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_DENY -> DENY
                ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_SKIP -> SKIP
                ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE -> OVERWRITE
                ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE_NEWER -> OVERWRITE_NEWER
                ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_OVERWRITE_CHANGED -> OVERWRITE_CHANGED
                ProtoTemplateJobConflictMode.TEMPLATE_JOB_CONFLICT_MODE_UNSPECIFIED,
                ProtoTemplateJobConflictMode.UNRECOGNIZED -> SKIP
            }
    }
}
