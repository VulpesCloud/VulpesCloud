package de.vulpescloud.node.db.impl.mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.vulpescloud.node.db.Database
import de.vulpescloud.node.utils.PropertyUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory

class MongoDBDatabase(
    override val name: String,
    val database: MongoDatabase,
    collectionPrefix: String,
) : Database {

    val collection by lazy { database.getCollection<MongoKVModel>(collectionName) }
    val collectionName = collectionPrefix + name
    private val logger = LoggerFactory.getLogger(MongoDBDatabase::class.java)

    private inline fun <T> measureTime(operation: String, block: () -> T): T {
        if (!PropertyUtils.isDBTiming()) return block()
        val start = System.nanoTime()
        val result = block()
        val duration = (System.nanoTime() - start) / 1_000_000.0
        logger.info("MongoDB[$collectionName]> $operation took ${duration}ms")
        return result
    }

    init {
        runBlocking { database.createCollection(collectionName) }
    }

    override suspend fun upsert(key: String, value: JsonElement) {
        if (PropertyUtils.isMoreDBLogging())
            logger.info("MongoDB[$collectionName]> Upserting $key -> $value")
        measureTime("upsert($key)") {
            collection.replaceOne(
                Filters.eq("key", key),
                MongoKVModel(key, value),
                ReplaceOptions().upsert(true),
            )
        }
    }

    override suspend fun insert(key: String, value: JsonElement) {
        if (PropertyUtils.isMoreDBLogging())
            logger.info("MongoDB[$collectionName]> Inserting $key -> $value")
        measureTime("insert($key)") {
            val exists = collection.countDocuments(Filters.eq("key", key)) > 0
            if (!exists) {
                collection.insertOne(MongoKVModel(key, value))
            }
        }
    }

    override suspend fun get(key: String): JsonElement? {
        if (PropertyUtils.isMoreDBLogging()) logger.info("MongoDB[$collectionName]> Getting $key")
        return measureTime("get($key)") {
            collection.find(Filters.eq("key", key)).firstOrNull()?.value
        }
    }

    override suspend fun delete(key: String) {
        if (PropertyUtils.isMoreDBLogging()) logger.info("MongoDB[$collectionName]> Deleting $key")
        measureTime("delete($key)") { collection.deleteOne(Filters.eq("key", key)) }
    }

    override suspend fun getAll(): List<JsonElement> {
        if (PropertyUtils.isMoreDBLogging()) logger.info("MongoDB[$collectionName]> Getting all")
        return measureTime("getAll()") {
            val list = mutableListOf<JsonElement>()
            collection.find().collect { list.add(it.value) }
            list
        }
    }

    override suspend fun find(filter: String): List<JsonElement> {
        if (PropertyUtils.isMoreDBLogging())
            logger.info("MongoDB[$collectionName]> Finding $filter")
        return measureTime("find($filter)") {
            collection.find(Filters.eq("key", filter)).map { it.value }.toList()
        }
    }
}
