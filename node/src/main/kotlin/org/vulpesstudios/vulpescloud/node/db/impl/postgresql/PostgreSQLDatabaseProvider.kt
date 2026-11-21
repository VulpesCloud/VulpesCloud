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

package org.vulpesstudios.vulpescloud.node.db.impl.postgresql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.vulpesstudios.vulpescloud.node.config.db.PostgreSQLConfig
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import kotlin.io.path.Path
import kotlin.io.path.exists

class PostgreSQLDatabaseProvider : DatabaseProvider {
    lateinit var database: Database
    private lateinit var dataSource: HikariDataSource

    override fun sqlDatabase(): Database = database

    override fun initialize() {
        val options = readConfig()
        val hikari =
            HikariConfig().apply {
                jdbcUrl = options.jdbcUrl
                driverClassName = "org.postgresql.Driver"
                username = options.user
                password = options.password
            }
        dataSource = HikariDataSource(hikari)
        val flyway =
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/postgresql")
                .load()
        flyway.migrate()
        database = Database.connect(dataSource)
    }

    override fun close() {
        if (::database.isInitialized) TransactionManager.closeAndUnregister(database)
        if (::dataSource.isInitialized) dataSource.close()
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
