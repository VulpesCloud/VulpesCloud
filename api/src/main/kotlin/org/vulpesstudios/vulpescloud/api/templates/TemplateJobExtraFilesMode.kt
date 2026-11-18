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

import build.buf.gen.vulpescloud.templates.v1.TemplateJobExtraFilesMode as ProtoTemplateJobExtraFilesMode

enum class TemplateJobExtraFilesMode {
    KEEP,
    PURGE;

    fun toDefinition(): ProtoTemplateJobExtraFilesMode =
        when (this) {
            KEEP -> ProtoTemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_KEEP
            PURGE -> ProtoTemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_PURGE
        }

    companion object {
        fun fromDefinition(proto: ProtoTemplateJobExtraFilesMode): TemplateJobExtraFilesMode =
            when (proto) {
                ProtoTemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_KEEP -> KEEP
                ProtoTemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_PURGE -> PURGE
                ProtoTemplateJobExtraFilesMode.TEMPLATE_JOB_EXTRA_FILES_MODE_UNSPECIFIED,
                ProtoTemplateJobExtraFilesMode.UNRECOGNIZED -> KEEP
            }
    }
}
