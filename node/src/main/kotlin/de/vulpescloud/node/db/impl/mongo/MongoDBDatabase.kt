package de.vulpescloud.node.db.impl.mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.vulpescloud.node.db.Database
import de.vulpescloud.node.utils.PropertyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    init {
        runBlocking { database.createCollection(collectionName) }
    }

    override suspend fun upsert(key: String, value: JsonElement) {
        if (PropertyUtils.isMoreDBLogging())
            logger.info("MongoDB[$collectionName]> Upserting $key -> $value")
        withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            val result =
                collection.replaceOne(
                    Filters.eq("key", key),
                    MongoKVModel(key, value),
                    ReplaceOptions().upsert(true),
                )
            val duration = (System.nanoTime() - start) / 1_000_000.0
            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> upsert($key) took ${duration}ms")
            }
            result
        }
    }

    override suspend fun insert(key: String, value: JsonElement) {
        if (PropertyUtils.isMoreDBLogging())
            logger.info("MongoDB[$collectionName]> Inserting $key -> $value")
        withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            val exists = collection.countDocuments(Filters.eq("key", key)) > 0
            val result =
                if (!exists) {
                    collection.insertOne(MongoKVModel(key, value))
                } else {
                    null
                }
            val duration = (System.nanoTime() - start) / 1_000_000.0
            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> insert($key) took ${duration}ms")
            }
            result
        }
    }

    override suspend fun get(key: String): JsonElement? {
        if (PropertyUtils.isMoreDBLogging()) logger.info("MongoDB[$collectionName]> Getting $key")
        return withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            val result = collection.find(Filters.eq("key", key)).firstOrNull()?.value
            val duration = (System.nanoTime() - start) / 1_000_000.0
            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> get($key) took ${duration}ms")
            }
            result
        }
    }

    override suspend fun delete(key: String) {
        if (PropertyUtils.isMoreDBLogging()) logger.info("MongoDB[$collectionName]> Deleting $key")
        withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            val result = collection.deleteOne(Filters.eq("key", key))
            val duration = (System.nanoTime() - start) / 1_000_000.0
            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> delete($key) took ${duration}ms")
            }
            result
        }
    }

    override suspend fun getAll(): List<JsonElement> {
        if (PropertyUtils.isMoreDBLogging()) logger.info("MongoDB[$collectionName]> Getting all")
        return withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            val list = mutableListOf<JsonElement>()
            collection.find().collect { list.add(it.value) }
            val duration = (System.nanoTime() - start) / 1_000_000.0
            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> getAll() took ${duration}ms")
            }
            list
        }
    }

    override suspend fun find(filter: String): List<JsonElement> {
        if (PropertyUtils.isMoreDBLogging())
            logger.info("MongoDB[$collectionName]> Finding $filter")
        return withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            val result = collection.find(Filters.eq("key", filter)).map { it.value }.toList()
            val duration = (System.nanoTime() - start) / 1_000_000.0
            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> find($filter) took ${duration}ms")
            }
            result
        }
    }

    override suspend fun insertIgnore(key: String, value: JsonElement) {
        if (PropertyUtils.isMoreDBLogging())
            logger.info("MongoDB[$collectionName]> InsertIgnoring $key -> $value")
        withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            collection.updateOne(
                Filters.eq("key", key),
                Updates.setOnInsert("key", key).let {
                    Updates.combine(it, Updates.setOnInsert("value", value))
                },
                UpdateOptions().upsert(true),
            )
            val duration = (System.nanoTime() - start) / 1_000_000.0
            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> insertIgnore($key) took ${duration}ms")
            }
        }
    }
}
