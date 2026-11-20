package org.vulpesstudios.vulpescloud.node.db.impl.sql

import java.sql.Connection

/** Prevents an upgraded node from silently starting against an empty target before manual copy. */
object LegacyMigrationGuard {
    fun currentSchema(connection: Connection): String? = runCatching { connection.schema }.getOrNull()

    fun requireMigrationComplete(connection: Connection) {
        val schema = currentSchema(connection)
        val legacyTables = mutableSetOf<String>()
        connection.metaData.getTables(connection.catalog, schema, "%", arrayOf("TABLE")).use { tables ->
            while (tables.next()) {
                val table = tables.getString("TABLE_NAME")
                val columns = mutableSetOf<String>()
                connection.metaData.getColumns(connection.catalog, schema, table, "%").use { result ->
                    while (result.next()) columns += result.getString("COLUMN_NAME").lowercase()
                }
                if ("key" in columns && "value" in columns) legacyTables += table
            }
        }
        if (legacyTables.isEmpty()) return

        val migratedTables = mutableSetOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT source_collection FROM vc_migration_manifest").use { result ->
                while (result.next()) migratedTables += result.getString(1)
            }
        }
        val missing = legacyTables - migratedTables
        check(missing.isEmpty()) {
            "Manual SQL migration is incomplete. Stop the node and run docs/database-migration/<engine>.sql; " +
                "these legacy tables are not recorded as migrated: ${missing.sorted().joinToString()}."
        }
    }
}
