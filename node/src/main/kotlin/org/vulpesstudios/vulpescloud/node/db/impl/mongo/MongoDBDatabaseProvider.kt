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

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.config.db.MongoConfig
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.utils.PropertyUtils
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.exists

class MongoDBDatabaseProvider : DatabaseProvider {

    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase
    private var databases = mutableMapOf<String, MongoDBDatabase>()
    private lateinit var options: MongoConfig
    private val logger = LoggerFactory.getLogger(MongoDBDatabaseProvider::class.java)

    override fun initialize() {
        options = getMongoConfig()
        val connectionString = options.connectionString
        val settings =
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .applyToConnectionPoolSettings {
                    it.maxSize(50)
                    it.maxWaitTime(10, TimeUnit.SECONDS)
                }
                .applyToSocketSettings {
                    it.connectTimeout(10, TimeUnit.SECONDS)
                    it.readTimeout(10, TimeUnit.SECONDS)
                }
                .retryWrites(true)
                .build()

        client = MongoClient.create(settings)

        database = client.getDatabase(options.database)
    }

    override fun getOrCreateDatabase(name: String): MongoDBDatabase {
        if (PropertyUtils.isMoreDBLogging()) logger.info("Getting or creating database $name")
        return databases.getOrPut(name) {
            MongoDBDatabase(name, database, options.collectionPrefix)
        }
    }

    override fun hasDatabase(name: String): Boolean {
        throw UnsupportedOperationException("I was to lazy implementing this xD")
    }

    override fun deleteDatabase(name: String) {
        throw UnsupportedOperationException(
            "I was to lazy implementing this and i don't know if there is a method for that in MongoDB xD"
        )
    }

    fun getMongoConfig(): MongoConfig {
        val path = Path("local/database/mongo.config.json")
        if (!path.exists()) {
            val config = MongoConfig("mongodb://localhost:27017", "vulpescloud", "vc_")
            path.toFile().writeText(Json.encodeToString(config))
            return config
        } else {
            return Json.decodeFromString(path.toFile().readText())
        }
    }
}
