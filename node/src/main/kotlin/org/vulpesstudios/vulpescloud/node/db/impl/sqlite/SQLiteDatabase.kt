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

package org.vulpesstudios.vulpescloud.node.db.impl.sqlite

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.vulpesstudios.vulpescloud.node.db.Database
import org.vulpesstudios.vulpescloud.node.db.impl.sql.KeyValueTable

class SQLiteDatabase(
    override val name: String,
    private val database: org.jetbrains.exposed.v1.jdbc.Database,
) : Database {

    private val json = Json

    val table = KeyValueTable()

    override suspend fun upsert(key: String, value: JsonElement) {
        require(key.length <= 255) { "Key is too long (${key.length}), max is 255" }

        val encoded = json.encodeToString(JsonElement.serializer(), value)

        transaction(database) {
            table.upsert(table.namespace, table.key) {
                it[table.namespace] = name
                it[table.key] = key
                it[table.value] = encoded
            }
        }
    }

    override suspend fun insert(key: String, value: JsonElement) {
        val encoded = json.encodeToString(JsonElement.serializer(), value)

        transaction(database) {
            table.insert {
                it[table.namespace] = name
                it[table.key] = key
                it[table.value] = encoded
            }
        }
    }

    override suspend fun compareAndSet(key: String, expected: JsonElement?, value: JsonElement): Boolean =
        transaction(database) {
            val encoded = json.encodeToString(JsonElement.serializer(), value)
            if (expected == null) {
                table.insertIgnore {
                    it[table.namespace] = name
                    it[table.key] = key
                    it[table.value] = encoded
                }
                return@transaction table.selectAll().where { (table.namespace eq name) and (table.key eq key) }
                    .map { it[table.value] }.firstOrNull() == encoded
            }
            val expectedEncoded = json.encodeToString(JsonElement.serializer(), expected)
            val updated = table.update({ (table.namespace eq name) and (table.key eq key) and (table.value eq expectedEncoded) }) {
                it[table.value] = encoded
            }
            updated > 0 || table.selectAll().where { (table.namespace eq name) and (table.key eq key) }
                .map { it[table.value] }.firstOrNull() == expectedEncoded
        }

    override suspend fun delete(key: String) {
        transaction(database) { table.deleteWhere { (table.namespace eq name) and (table.key eq key) } }
    }

    override suspend fun get(key: String): JsonElement? {
        return transaction(database) {
            table
                .selectAll()
                .where { (table.namespace eq name) and (table.key eq key) }
                .map { it[table.value] }
                .firstOrNull()
                .let { json.decodeFromString(JsonElement.serializer(), it ?: return@let null) }
        }
    }

    override suspend fun getAll(): List<JsonElement> {
        return transaction(database) {
            table.selectAll().where { table.namespace eq name }.map {
                json.decodeFromString(JsonElement.serializer(), it[table.value])
            }
        }
    }

    override suspend fun getAllEntries(): List<Pair<String, JsonElement>> =
        transaction(database) {
            table.selectAll().where { table.namespace eq name }.map {
                it[table.key] to json.decodeFromString(JsonElement.serializer(), it[table.value])
            }
        }

    override suspend fun find(filter: String): List<JsonElement> {
        return transaction(database) {
            table
                .selectAll()
                .where { table.namespace eq name }
                .filter { it[table.key].contains(filter) }
                .map { json.decodeFromString(JsonElement.serializer(), it[table.value]) }
        }
    }

    override suspend fun insertIgnore(key: String, value: JsonElement) {
        transaction(database) {
            table.insertIgnore {
                it[table.namespace] = name
                it[table.key] = key
                it[table.value] = json.encodeToString(JsonElement.serializer(), value)
            }
        }
    }
}
