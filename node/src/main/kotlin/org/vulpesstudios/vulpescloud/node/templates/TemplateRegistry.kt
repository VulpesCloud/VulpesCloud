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

import build.buf.gen.vulpescloud.templates.v1.TemplateLocation
import build.buf.gen.vulpescloud.templates.v1.TemplateReference
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.vulpesstudios.vulpescloud.api.templates.Template
import org.vulpesstudios.vulpescloud.node.Node

/**
 * Cluster-wide metadata store for [Template]s (id/name/storage/location/version/enabled).
 *
 * This only stores metadata - it does not touch actual file contents, which live on whichever
 * [TemplateStorage] the template's [Template.location] points at (see [TemplateRegistry]).
 * Since this uses the main (shared) database, every node sees the same set of templates
 * regardless of where the template's files actually live.
 */
object TemplateRegistry {

    private val templatesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("templates")
    }

    suspend fun save(template: Template) {
        templatesDatabase.upsert(template.id, Json.encodeToJsonElement(template))
    }

    suspend fun delete(template: Template) {
        templatesDatabase.delete(template.id)
    }

    suspend fun getById(id: String): Template? {
        if (id.isBlank()) return null
        return runCatching { templatesDatabase.get(id) }
            .getOrNull()
            ?.let { Json.decodeFromJsonElement<Template>(it) }
    }

    suspend fun getAll(): List<Template> {
        return templatesDatabase.getAll().map { Json.decodeFromJsonElement<Template>(it) }
    }

    /**
     * Resolves a [TemplateReference] to the [Template] it points at. Prefers [TemplateReference.templateId]
     * when set, otherwise falls back to matching by [TemplateReference.name] (optionally narrowed
     * down further by [TemplateReference.location] fields, in case multiple templates share a name
     * across different storages/nodes).
     */
    suspend fun findByReference(reference: TemplateReference): Template? {
        if (reference.templateId.isNotBlank()) {
            getById(reference.templateId)?.let { return it }
        }
        if (reference.name.isBlank()) return null

        val candidates = getAll().filter { it.name == reference.name }
        if (candidates.size <= 1) return candidates.firstOrNull()

        return candidates.firstOrNull { matchesLocation(it.location.toDefinition(), reference.location) }
            ?: candidates.firstOrNull()
    }

    fun matchesLocation(actual: TemplateLocation, filter: TemplateLocation): Boolean {
        if (filter.nodeName.isNotBlank() && actual.nodeName != filter.nodeName) return false
        if (filter.storageName.isNotBlank() && actual.storageName != filter.storageName) return false
        return true
    }
}
