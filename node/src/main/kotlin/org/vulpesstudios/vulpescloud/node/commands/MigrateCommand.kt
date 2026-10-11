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

package org.vulpesstudios.vulpescloud.node.commands

import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.command.ConsoleCommandSource
import org.vulpesstudios.vulpescloud.node.command.annotation.SpecificCommandSource
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.mariadb.MariaDBDatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.mongo.MongoDBDatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.sqlite.SQLiteDatabaseProvider

@Suppress("UNUSED")
class MigrateCommand {
    @Command("migrate database <old_db> <new_db>")
    @SpecificCommandSource(ConsoleCommandSource::class)
    @Permission("database.migrate")
    fun migrateDatabase(
        source: CommandSource,
        @Argument("old_db") oldDatabase: String,
        @Argument("new_db") newDatabase: String,
        @Flag("force") force: Boolean,
    ) {
        val oldType = oldDatabase.lowercase()
        val newType = newDatabase.lowercase()
        if (oldType == newType) {
            source.sendMessage(
                "<red>Source and destination database types must be different.</red>"
            )
            return
        }
        val available = setOf("sqlite", "mariadb", "mongodb")
        if (oldType !in available || newType !in available) {
            source.sendMessage("<red>Supported database types: sqlite, mariadb, mongodb.</red>")
            return
        }
        try {
            val from = provider(oldType).also { it.initialize() }
            val to = provider(newType).also { it.initialize() }
            runBlocking {
                val hasData =
                    to.getDatabaseNames().any {
                        to.getOrCreateDatabase(it).getAllEntries().isNotEmpty()
                    }
                if (hasData && !force) {
                    source.sendMessage(
                        "<red>Migration stopped: the destination contains data. Use --force to continue.</red>"
                    )
                    return@runBlocking
                }
                var copied = 0
                from.getDatabaseNames().forEach { name ->
                    val target = to.getOrCreateDatabase(name)
                    from.getOrCreateDatabase(name).getAllEntries().forEach { (key, value) ->
                        target.upsert(key, value)
                        copied++
                    }
                }
                Node.instance.moduleProvider.getAllModules().forEach {
                    it.module.onDatabaseMigration(from, to)
                }

                from.close()
                to.close()

                source.sendMessage(
                    "<green>Database migration completed: copied $copied records from $oldType to $newType.</green>"
                )
            }
        } catch (exception: Exception) {
            source.sendMessage(
                "<red>Database migration failed: ${exception.message ?: exception.javaClass.simpleName}</red>"
            )
            throw exception
        }
    }

    private fun provider(type: String): DatabaseProvider =
        when (type) {
            "sqlite" -> SQLiteDatabaseProvider()
            "mariadb" -> MariaDBDatabaseProvider()
            "mongodb" -> MongoDBDatabaseProvider()
            else -> error("Unsupported database provider: $type")
        }
}
