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

import build.buf.gen.vulpescloud.templates.v1.TemplateDefinition
import kotlinx.serialization.Serializable

@Serializable
data class Template(
    val name: String,
    val weight: Int,
    val id: String = "",
    val location: TemplateLocation,
    val version: String = "",
    val enabled: Boolean = true,
) {

    fun toDefinition(): TemplateDefinition {
        val builder =
            TemplateDefinition.newBuilder()
                .setId(id)
                .setName(name)
                .setWeight(weight)
                .setLocation(location.toDefinition())
                .setVersion(version)
                .setEnabled(enabled)

        return builder.build()
    }

    companion object {
        fun fromDefinition(templateDefinition: TemplateDefinition): Template {
            return Template(
                templateDefinition.name,
                templateDefinition.weight,
                templateDefinition.id,
                TemplateLocation.fromDefinition(templateDefinition.location),
                templateDefinition.version,
                templateDefinition.enabled,
            )
        }

    }
}
