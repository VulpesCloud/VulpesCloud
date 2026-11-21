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
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import kotlin.io.path.Path

class SQLiteDatabaseProvider : DatabaseProvider {

    lateinit var database: Database
    private lateinit var dataSource: HikariDataSource

    override fun sqlDatabase(): Database = database

    override fun initialize() {
        Path("local/database").toFile().mkdirs()
        val jdbcUrl = "jdbc:sqlite:local/database/database.db"
        dataSource = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.sqlite.JDBC"
            connectionInitSql = "PRAGMA foreign_keys=ON"
        })
        Flyway.configure().dataSource(dataSource)
            .locations("classpath:db/migration/sqlite")
            .load().migrate()
        database = Database.connect(dataSource)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }

    override fun close() {
        TransactionManager.closeAndUnregister(database)
        dataSource.close()
    }

}
