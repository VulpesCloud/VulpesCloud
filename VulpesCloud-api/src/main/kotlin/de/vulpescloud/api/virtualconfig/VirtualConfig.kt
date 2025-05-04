package de.vulpescloud.api.virtualconfig

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.json.JsonFormat
import de.vulpescloud.api.mysql.TaskTable
import de.vulpescloud.api.mysql.VirtualConfigTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.json.JSONObject
import org.slf4j.LoggerFactory
import kotlin.io.path.Path

class VirtualConfig(val name: String) {

    private val path = Path("temp-configs/$name.json")
    private val config = FileConfig.builder(path, JsonFormat.fancyInstance()).sync().build()

    private val logger = LoggerFactory.getLogger("VirtualConfig - $name")

    init {
        transaction {
            SchemaUtils.create(VirtualConfigTable)
            val row = VirtualConfigTable.select(VirtualConfigTable.name eq name).firstOrNull()
            if (row != null) {
                val data = row[TaskTable.json]

                path.toFile().writeText(JSONObject(data).toString(4))
            }
        }

        config.load()
    }

    /**
     * Returns the Value from the Entry and sets the Value if it does not exist
     */
    fun <T : Any> getEntry(entry: String, default: T, addIfMissing: Boolean = true): T {
        if (config.contains(entry)) {
            return config.get(entry)
        } else {
            if (addIfMissing) {
                config.set<T>(entry, default)
                config.save()
                return default
            }
            logger.warn(
                "VirtualConfig - $name: Trying to get entry $entry but it won't be added to the config file!"
            )
            return default
        }
    }

    /**
     * Sets the Entry specified to a Value
     * NOTE: This will not update the config in the Database, you have to manually call publish()
     */
    fun <T : Any> setEntry(entry: String, value: T) {
        config.set<T>(entry, value)
    }

    /**
     * Pulls the config from MySQL
     */
    fun pull() {
        transaction {
            SchemaUtils.create(VirtualConfigTable)
            val row = VirtualConfigTable.select(VirtualConfigTable.name eq name).firstOrNull()
            if (row != null) {
                val data = row[TaskTable.json]

                path.toFile().writeText(JSONObject(data).toString(4))

                config.load()
            } else {
                logger.error(
                    "VirtualConfig - $name: Tried to pull config from Database but it was not found!"
                )
            }
        }
    }

    /**
     * Publishes the current config to the MySQL Database
     */
    fun publish() {
        transaction {
            SchemaUtils.create(VirtualConfigTable)
            VirtualConfigTable.update({ VirtualConfigTable.name eq name }) {
                it[json] = JSONObject(path.toFile().readText()).toString()
            }
        }
    }

    /**
     * Reloads the data from the Json File
     */
    fun loadLocalChanges() {
        config.load()
    }
}
