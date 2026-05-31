package de.vulpescloud.node.db

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
