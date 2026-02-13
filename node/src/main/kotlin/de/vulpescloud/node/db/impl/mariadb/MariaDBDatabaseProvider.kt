package de.vulpescloud.node.db.impl.mariadb

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.vulpescloud.node.config.db.MariaDBConfig
import de.vulpescloud.node.db.DatabaseProvider
import de.vulpescloud.node.db.impl.sql.KeyValueTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.io.path.Path
import kotlin.io.path.exists

class MariaDBDatabaseProvider : DatabaseProvider {

    lateinit var database: Database
    private val databases = mutableMapOf<String, MariaDBDatabase>()

    override fun initialize() {
        val options = getMariaDBConfig()
        val hikariConfig = HikariConfig()

        hikariConfig.jdbcUrl = "jdbc:mariadb://${options.host}:${options.port}/${options.database}"
        hikariConfig.driverClassName = "org.mariadb.jdbc.Driver"
        hikariConfig.username = options.user
        hikariConfig.password = options.password

        val dataSource = HikariDataSource(hikariConfig)
        database = Database.connect(dataSource)
    }

    override fun getOrCreateDatabase(name: String): MariaDBDatabase {
        return databases.getOrPut(name) { MariaDBDatabase(name, database) }
    }

    override fun hasDatabase(name: String): Boolean {
        return transaction(database) { SchemaUtils.listTables().any { it == name } }
    }

    override fun deleteDatabase(name: String) {
        if (hasDatabase(name)) {
            val tableToDelete = databases[name]?.table ?: KeyValueTable(name)
            transaction(database) { SchemaUtils.drop(tableToDelete) }
            databases.remove(name)
        }
    }

    private fun getMariaDBConfig(): MariaDBConfig {
        val configPath = Path("local/database/mariadb.config.json")
        if (!configPath.exists()) {
            val config = MariaDBConfig("vulpescloud", "password", "localhost", 3306, "vulpescloud")
            configPath.toFile().writeText(Json.encodeToString(config))
            return config
        } else {
            return Json.decodeFromString(configPath.toFile().readText())
        }
    }
}
