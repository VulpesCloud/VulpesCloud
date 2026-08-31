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

package org.vulpesstudios.vulpescloud.node.db

import kotlinx.serialization.json.JsonElement

interface Database {

    val name: String

    suspend fun upsert(key: String, value: JsonElement)

    suspend fun insert(key: String, value: JsonElement)

    suspend fun delete(key: String)

    suspend fun get(key: String): JsonElement?

    suspend fun find(filter: String): List<JsonElement>

    suspend fun getAll(): List<JsonElement>

    suspend fun insertIgnore(key: String, value: JsonElement)

}
