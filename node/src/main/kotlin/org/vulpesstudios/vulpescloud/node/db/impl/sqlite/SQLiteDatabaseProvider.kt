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

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.sql.KeyValueTable
import java.sql.Connection
import kotlin.io.path.Path

class SQLiteDatabaseProvider : DatabaseProvider {

    lateinit var database: Database
    private val databases = mutableMapOf<String, SQLiteDatabase>()

    override fun initialize() {
        Path("local/database").toFile().mkdirs()
        database = Database.connect("jdbc:sqlite:local/database/database.db", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }

    override fun getOrCreateDatabase(name: String): SQLiteDatabase {
        return databases.getOrPut(name) { SQLiteDatabase(name, database) }
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
}
