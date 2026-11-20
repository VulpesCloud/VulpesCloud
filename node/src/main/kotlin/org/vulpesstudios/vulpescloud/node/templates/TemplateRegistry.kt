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
import org.vulpesstudios.vulpescloud.api.templates.Template
import org.vulpesstudios.vulpescloud.node.db.TemplateRepository

/**
 * Cluster-wide metadata store for [Template]s (id/name/storage/location/version/enabled).
 *
 * This only stores metadata - it does not touch actual file contents, which live on whichever
 * [TemplateStorage] the template's [Template.location] points at (see [TemplateRegistry]).
 * Since this uses the main (shared) database, every node sees the same set of templates
 * regardless of where the template's files actually live.
 */
object TemplateRegistry {

    fun save(template: Template) { TemplateRepository.save(template) }

    /** Inserts a new template without replacing an existing template with the same id. */
    fun create(template: Template): Boolean = TemplateRepository.save(template, createOnly = true)

    fun delete(template: Template) {
        TemplateRepository.delete(template.id)
    }

    fun getById(id: String): Template? {
        if (id.isBlank()) return null
        return runCatching { TemplateRepository.get(id) }.getOrNull()
    }

    fun getAll(): List<Template> {
        return TemplateRepository.getAll()
    }

    /**
     * Resolves a [TemplateReference] to the [Template] it points at. Prefers [TemplateReference.templateId]
     * when set, otherwise falls back to matching by [TemplateReference.name] (optionally narrowed
     * down further by [TemplateReference.location] fields, in case multiple templates share a name
     * across different storages/nodes).
     */
    fun findByReference(reference: TemplateReference): Template? {
        if (reference.templateId.isNotBlank()) {
            return getById(reference.templateId)?.takeIf {
                matchesLocation(it.location.toDefinition(), reference.location)
            }
        }
        if (reference.name.isBlank()) return null

        val candidates = getAll().filter { it.name == reference.name }
        val locationMatches = candidates.filter {
            matchesLocation(it.location.toDefinition(), reference.location)
        }
        // A name-only (or stale location) reference is ambiguous when templates with that name
        // exist on multiple hosts/storages. Never silently route it to an arbitrary backend.
        return locationMatches.singleOrNull()
    }

    fun matchesLocation(actual: TemplateLocation, filter: TemplateLocation): Boolean {
        if (filter.nodeName.isNotBlank() && actual.nodeName != filter.nodeName) return false
        if (filter.storageName.isNotBlank() && actual.storageName != filter.storageName) return false
        return true
    }
}
