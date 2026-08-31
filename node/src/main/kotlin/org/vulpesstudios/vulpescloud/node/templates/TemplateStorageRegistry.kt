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

import build.buf.gen.vulpescloud.templates.v1.TemplateStorageType

object TemplateStorageRegistry {

    private val storages = mutableListOf<TemplateStorage>()

    fun getAllTemplateStorages(type: TemplateStorageType?): List<TemplateStorage> {
        if (type == null) return storages
        return storages.filter { it.type().name == type.name }
    }

    fun registerTemplateStorage(storage: TemplateStorage) {
        if (storages.find { it.name() == storage.name() } != null) throw IllegalArgumentException("Template storage ${storage.name()} is already registered.")
        storages.add(storage)
    }

    fun getTemplateStorageByName(name: String): TemplateStorage? {
        return storages.find { it.name() == name }
    }

    fun unregisterTemplateStorage(storage: TemplateStorage) {
        storages.remove(storage)
    }

}
