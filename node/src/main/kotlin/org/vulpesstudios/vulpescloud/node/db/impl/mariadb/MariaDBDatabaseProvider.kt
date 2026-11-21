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

package org.vulpesstudios.vulpescloud.node.db.impl.mariadb

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database as ExposedDatabase
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.vulpesstudios.vulpescloud.node.config.db.MariaDBConfig
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import kotlin.io.path.Path
import kotlin.io.path.exists

class MariaDBDatabaseProvider : DatabaseProvider {

    lateinit var database: ExposedDatabase
    lateinit var dataSource: HikariDataSource
    override fun sqlDatabase(): ExposedDatabase = database

    override fun initialize() {
        val options = getMariaDBConfig()
        val hikariConfig = HikariConfig()

        hikariConfig.jdbcUrl = "jdbc:mariadb://${options.host}:${options.port}/${options.database}"
        hikariConfig.driverClassName = "org.mariadb.jdbc.Driver"
        hikariConfig.username = options.user
        hikariConfig.password = options.password

        dataSource = HikariDataSource(hikariConfig)
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/mariadb")
            .load().migrate()
        database = ExposedDatabase.connect(dataSource)
    }

    override fun close() {
        TransactionManager.closeAndUnregister(database)
        dataSource.close()
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
