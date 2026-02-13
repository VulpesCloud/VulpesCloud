package de.vulpescloud.node.db.impl.sqlite

import de.vulpescloud.node.db.Database
import de.vulpescloud.node.db.impl.sql.KeyValueTable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

class SQLiteDatabase(
    override val name: String,
    private val database: org.jetbrains.exposed.v1.jdbc.Database,
) : Database {

    private val json = Json

    val table = KeyValueTable(name)

    init {
        transaction(database) { SchemaUtils.create(table) }
    }

    override suspend fun upsert(key: String, value: JsonElement) {
        require(key.length <= 255) { "Key is too long (${key.length}), max is 255" }

        val encoded = json.encodeToString(JsonElement.serializer(), value)

        transaction(database) {
            table.upsert(table.key) {
                it[table.key] = key
                it[table.value] = encoded
            }
        }
    }

    override suspend fun insert(key: String, value: JsonElement) {
        val encoded = json.encodeToString(JsonElement.serializer(), value)

        transaction(database) {
            table.insert {
                it[table.key] = key
                it[table.value] = encoded
            }
        }
    }

    override suspend fun delete(key: String) {
        transaction(database) { table.deleteWhere { table.key eq key } }
    }

    override suspend fun get(key: String): JsonElement? {
        return transaction(database) {
            table
                .selectAll()
                .where { table.key eq key }
                .map { it[table.value] }
                .firstOrNull()
                .let { json.decodeFromString(JsonElement.serializer(), it ?: return@let null) }
        }
    }

    override suspend fun getAll(): List<JsonElement> {
        return transaction(database) {
            table.selectAll().map {
                json.decodeFromString(JsonElement.serializer(), it[table.value])
            }
        }
    }

    override suspend fun find(filter: String): List<JsonElement> {
        return transaction(database) {
            table.selectAll().filter { it[table.key].contains(filter) }.map {
                json.decodeFromString(JsonElement.serializer(), it[table.value])
            }
        }
    }
}
