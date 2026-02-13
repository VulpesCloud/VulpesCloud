package de.vulpescloud.node.db.impl.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.vulpescloud.node.config.db.MongoConfig
import de.vulpescloud.node.db.DatabaseProvider
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.exists

class MongoDBDatabaseProvider : DatabaseProvider {

    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase
    private var databases = mutableMapOf<String, MongoDBDatabase>()
    private lateinit var options: MongoConfig

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
