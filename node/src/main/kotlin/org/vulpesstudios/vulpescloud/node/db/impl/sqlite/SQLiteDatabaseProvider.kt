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

package org.vulpesstudios.vulpescloud.node.db.impl.sqlite

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.sql.KeyValueTable
import org.vulpesstudios.vulpescloud.node.db.impl.sql.LegacyMigrationGuard
import java.sql.Connection
import kotlin.io.path.Path

class SQLiteDatabaseProvider : DatabaseProvider {

    lateinit var database: Database
    private val databases = mutableMapOf<String, SQLiteDatabase>()

    override fun initialize() {
        Path("local/database").toFile().mkdirs()
        val jdbcUrl = "jdbc:sqlite:local/database/database.db"
        val existingTables = java.sql.DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.metaData.getTables(null, null, "%", arrayOf("TABLE")).use { rs ->
                buildSet { while (rs.next()) add(rs.getString("TABLE_NAME")) }
            }
        }
        Flyway.configure().dataSource(jdbcUrl, null, null)
            .locations("classpath:db/migration/sqlite")
            .baselineOnMigrate(existingTables.any { !it.equals("flyway_schema_history", true) })
            .baselineVersion("0").load().migrate()
        java.sql.DriverManager.getConnection(jdbcUrl).use(LegacyMigrationGuard::requireMigrationComplete)
        database = Database.connect(jdbcUrl, "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }

    override fun close() {
        TransactionManager.closeAndUnregister(database)
    }

    override fun getOrCreateDatabase(name: String): SQLiteDatabase {
        return databases.getOrPut(name) { SQLiteDatabase(name, database) }
    }

    override fun hasDatabase(name: String): Boolean {
        val records = KeyValueTable()
        return transaction(database) { records.selectAll().any { it[records.namespace] == name } }
    }

    override fun getDatabaseNames(): Set<String> {
        val records = KeyValueTable()
        return transaction(database) { records.selectAll().map { it[records.namespace] }.toSet() }
    }

    override fun deleteDatabase(name: String) {
        val records = KeyValueTable()
        transaction(database) { records.deleteWhere { records.namespace eq name } }
        databases.remove(name)
    }
}
