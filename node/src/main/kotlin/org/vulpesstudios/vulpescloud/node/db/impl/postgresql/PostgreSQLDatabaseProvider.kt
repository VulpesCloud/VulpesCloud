package org.vulpesstudios.vulpescloud.node.db.impl.postgresql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.vulpesstudios.vulpescloud.node.config.db.PostgreSQLConfig
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.sql.KeyValueTable
import org.vulpesstudios.vulpescloud.node.db.impl.sql.LegacyMigrationGuard
import kotlin.io.path.Path
import kotlin.io.path.exists

class PostgreSQLDatabaseProvider : DatabaseProvider {
    lateinit var database: Database
    private lateinit var dataSource: HikariDataSource
    private val databases = mutableMapOf<String, PostgreSQLDatabase>()

    override fun initialize() {
        val options = readConfig()
        val hikari = HikariConfig().apply {
            jdbcUrl = options.jdbcUrl
            driverClassName = "org.postgresql.Driver"
            username = options.user
            password = options.password
        }
        dataSource = HikariDataSource(hikari)
        val existingTables = dataSource.connection.use { connection ->
            connection.metaData.getTables(connection.catalog, LegacyMigrationGuard.currentSchema(connection), "%", arrayOf("TABLE")).use { rs ->
                buildSet { while (rs.next()) add(rs.getString("TABLE_NAME")) }
            }
        }
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/postgresql")
            .baselineOnMigrate(existingTables.any { !it.equals("flyway_schema_history", true) })
            .baselineVersion("0")
            .load()
        flyway.migrate()
        dataSource.connection.use(LegacyMigrationGuard::requireMigrationComplete)
        database = Database.connect(dataSource)
    }

    override fun close() {
        if (::database.isInitialized) TransactionManager.closeAndUnregister(database)
        if (::dataSource.isInitialized) dataSource.close()
    }

    override fun getOrCreateDatabase(name: String): PostgreSQLDatabase =
        databases.getOrPut(name) { PostgreSQLDatabase(name, database) }

    override fun hasDatabase(name: String): Boolean {
        val records = KeyValueTable()
        return transaction(database) { records.selectAll().any { it[records.namespace] == name } }
    }

    override fun getDatabaseNames(): Set<String> {
        val records = KeyValueTable()
        return transaction(database) {
            records.selectAll().map { it[records.namespace] }.toSet()
        }
    }

    override fun deleteDatabase(name: String) {
        val records = KeyValueTable()
        transaction(database) { records.deleteWhere { records.namespace eq name } }
        databases.remove(name)
    }

    private fun readConfig(): PostgreSQLConfig {
        val path = Path("local/database/postgresql.config.json")
        if (!path.exists()) {
            path.toFile().parentFile.mkdirs()
            val defaults = PostgreSQLConfig()
            path.toFile().writeText(Json.encodeToString(defaults))
            return defaults
        }
        return Json.decodeFromString(path.toFile().readText())
    }

}
