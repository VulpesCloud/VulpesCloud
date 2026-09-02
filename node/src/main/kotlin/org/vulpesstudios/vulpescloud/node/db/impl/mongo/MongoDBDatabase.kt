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

package org.vulpesstudios.vulpescloud.node.db.impl.mongo

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import org.bson.BsonDocument
import org.bson.BsonValue
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.db.Database
import org.vulpesstudios.vulpescloud.node.utils.PropertyUtils

class MongoDBDatabase(
    override val name: String,
    val database: MongoDatabase,
    collectionPrefix: String,
) : Database {

    val collectionName = collectionPrefix + name

    val collection by lazy {
        database.getCollection<MongoKVModel>(collectionName)
    }

    private val logger = LoggerFactory.getLogger(MongoDBDatabase::class.java)

    init {
        runBlocking {
            database.createCollection(collectionName)

            collection.createIndex(
                Indexes.ascending("key"),
                IndexOptions().unique(true),
            )
        }
    }

    private fun JsonElement.toBsonValue(): BsonValue =
        BsonDocument.parse("""{"value":$this}""")["value"]!!

    override suspend fun upsert(key: String, value: JsonElement) {
        if (PropertyUtils.isMoreDBLogging()) {
            logger.info("MongoDB[$collectionName]> Upserting $key -> $value")
        }

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
        if (PropertyUtils.isMoreDBLogging()) {
            logger.info("MongoDB[$collectionName]> Inserting $key -> $value")
        }

        withContext(Dispatchers.IO) {
            val start = System.nanoTime()

            val result =
                try {
                    collection.insertOne(MongoKVModel(key, value))
                } catch (_: com.mongodb.MongoWriteException) {
                    null
                }

            val duration = (System.nanoTime() - start) / 1_000_000.0

            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> insert($key) took ${duration}ms")
            }

            result
        }
    }

    override suspend fun compareAndSet(
        key: String,
        expected: JsonElement?,
        value: JsonElement,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val filter =
                if (expected == null) {
                    Filters.eq("key", key)
                } else {
                    Filters.and(
                        Filters.eq("key", key),
                        Filters.eq("value", expected.toBsonValue()),
                    )
                }

            val result =
                collection.replaceOne(
                    filter,
                    MongoKVModel(key, value),
                    ReplaceOptions().upsert(expected == null),
                )

            result.modifiedCount > 0L || (expected == null && result.upsertedId != null)
        }

    override suspend fun get(key: String): JsonElement? {
        if (PropertyUtils.isMoreDBLogging()) {
            logger.info("MongoDB[$collectionName]> Getting $key")
        }

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
        if (PropertyUtils.isMoreDBLogging()) {
            logger.info("MongoDB[$collectionName]> Deleting $key")
        }

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
        if (PropertyUtils.isMoreDBLogging()) {
            logger.info("MongoDB[$collectionName]> Getting all")
        }

        return withContext(Dispatchers.IO) {
            val start = System.nanoTime()

            val list = mutableListOf<JsonElement>()

            collection.find().collect {
                list.add(it.value)
            }

            val duration = (System.nanoTime() - start) / 1_000_000.0

            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> getAll() took ${duration}ms")
            }

            list
        }
    }

    override suspend fun find(filter: String): List<JsonElement> {
        if (PropertyUtils.isMoreDBLogging()) {
            logger.info("MongoDB[$collectionName]> Finding $filter")
        }

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

    override suspend fun insertIgnore(
        key: String,
        value: JsonElement,
    ) {
        if (PropertyUtils.isMoreDBLogging()) {
            logger.info("MongoDB[$collectionName]> InsertIgnoring $key -> $value")
        }

        withContext(Dispatchers.IO) {
            val start = System.nanoTime()

            collection.updateOne(
                Filters.eq("key", key),
                Updates.combine(
                    Updates.setOnInsert("key", key),
                    Updates.setOnInsert(
                        "value",
                        value.toBsonValue(),
                    ),
                ),
                UpdateOptions().upsert(true),
            )

            val duration = (System.nanoTime() - start) / 1_000_000.0

            if (PropertyUtils.isDBTiming()) {
                logger.info("MongoDB[$collectionName]> insertIgnore($key) took ${duration}ms")
            }
        }
    }
}
