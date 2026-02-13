package de.vulpescloud.node.db.impl.mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.vulpescloud.node.db.Database
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement

class MongoDBDatabase(
    override val name: String,
    val database: MongoDatabase,
    val collectionPrefix: String,
) : Database {

    val collection by lazy { database.getCollection<MongoKVModel>(collectionName) }
    val collectionName = collectionPrefix + name

    init {
        runBlocking { database.createCollection(collectionName) }
    }

    override suspend fun upsert(key: String, value: JsonElement) {
        collection.replaceOne(
            Filters.eq("key", key),
            MongoKVModel(key, value),
            ReplaceOptions().upsert(true),
        )
    }

    override suspend fun insert(key: String, value: JsonElement) {
        collection.updateOne(
            Filters.eq("key", key),
            Updates.combine(
                Updates.setOnInsert("key", key),
                Updates.setOnInsert("value", value.toString()),
            ),
            UpdateOptions().upsert(true),
        )
    }

    override suspend fun get(key: String): JsonElement? {
        return collection.find(Filters.eq("key", key)).firstOrNull()?.value
    }

    override suspend fun delete(key: String) {
        collection.deleteOne(Filters.eq("key", key))
    }

    override suspend fun getAll(): List<JsonElement> {
        val list = mutableListOf<JsonElement>()
        collection.find().collect { list.add(it.value) }
        return list
    }

    override suspend fun find(filter: String): List<JsonElement> {
        return collection.find(Filters.eq("key", filter)).map { it.value }.toList()
    }
}
