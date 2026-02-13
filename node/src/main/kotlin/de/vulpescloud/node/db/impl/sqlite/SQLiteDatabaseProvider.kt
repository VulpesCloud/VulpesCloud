package de.vulpescloud.node.db.impl.sqlite

import de.vulpescloud.node.db.DatabaseProvider
import de.vulpescloud.node.db.impl.sql.KeyValueTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
