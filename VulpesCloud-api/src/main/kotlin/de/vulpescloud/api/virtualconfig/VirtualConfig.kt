package de.vulpescloud.api.virtualconfig

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.json.JsonFormat
import de.vulpescloud.api.mysql.VirtualConfigTable
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import kotlin.io.path.Path
import kotlin.properties.Delegates
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.json.JSONObject
import org.slf4j.LoggerFactory

class VirtualConfig(val name: String, var description: String = "none") {

    private val path = Path("temp-configs/$name.json")
    private val config = FileConfig.builder(path, JsonFormat.fancyInstance()).sync().build()
    private lateinit var virtualConfigReloadChannelListener: VirtualConfigReloadChannelListener

    var lastModified by Delegates.notNull<Long>()
        private set

    private val logger = LoggerFactory.getLogger("VirtualConfig - $name")

    /**
     * Initializes the VirtualConfig This will create the temp file and load the config from MySQL
     * or create an empty config
     */
    fun init(defaultConfig: JSONObject = JSONObject()) {
        path.parent.toFile().mkdirs()
        transaction {
            SchemaUtils.create(VirtualConfigTable)
            val row =
                VirtualConfigTable.selectAll()
                    .where { VirtualConfigTable.name eq name }
                    .firstOrNull()
            if (row != null) {
                row[VirtualConfigTable.json].let { data ->
                    path.toFile().writeText(JSONObject(data).toString(4))
                }
                lastModified = row[VirtualConfigTable.lastModified]
                description = row[VirtualConfigTable.description]
            } else {
                lastModified = System.currentTimeMillis()
                path.toFile().writeText(defaultConfig.toString(4))
            }
        }

        config.load()

        virtualConfigReloadChannelListener = VirtualConfigReloadChannelListener(this)
    }

    /**
     * Returns the Value from the Entry and sets the Value if it does not exist NOTE: This will not
     * update the config in the Database, you have to manually call publish() if you want to add the
     * missing entry
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
     * Sets the Entry specified to a Value NOTE: This will not update the config in the Database,
     * you have to manually call publish()
     */
    fun <T : Any> setEntry(entry: String, value: T) {
        config.set<T>(entry, value)
        config.save()
    }

    /** Pulls the config from MySQL */
    fun pull() {
        transaction {
            SchemaUtils.create(VirtualConfigTable)
            val row =
                VirtualConfigTable.selectAll()
                    .where { VirtualConfigTable.name eq name }
                    .firstOrNull()
            if (row != null) {
                val data = row[VirtualConfigTable.json]

                path.toFile().writeText(JSONObject(data).toString(4))

                config.load()

                lastModified = row[VirtualConfigTable.lastModified]
            } else {
                logger.error(
                    "VirtualConfig - $name: Tried to pull config from Database but it was not found!"
                )
            }
        }
    }

    /** Publishes the current config to the MySQL Database */
    fun publish() {
        val configName = name
        transaction {
            SchemaUtils.create(VirtualConfigTable)

            val row = VirtualConfigTable.select(VirtualConfigTable.name eq name).firstOrNull()

            if (row != null) {
                val updateTime = System.currentTimeMillis()
                VirtualConfigTable.update({ VirtualConfigTable.name eq name }) {
                    it[json] = JSONObject(path.toFile().readText()).toString()
                    it[lastModified] = updateTime
                }

                lastModified = updateTime
            } else {
                val updateTime = System.currentTimeMillis()
                VirtualConfigTable.insert {
                    it[name] = configName
                    it[description] = description
                    it[lastModified] = updateTime
                    it[json] = JSONObject(path.toFile().readText()).toString()
                }
                lastModified = updateTime
            }
        }

        getRC()?.sendMessage("VCONFIG_RELOAD_$name", "VCONFIG_RELOAD_$name")
    }

    /**
     * Reloads the data from the JSON File NOTE: This won't update the config in the Database, you
     * have to manually call publish()
     */
    fun loadLocalChanges() {
        config.load()
    }

    /** Closes the config and deletes the temp file */
    fun close() {
        virtualConfigReloadChannelListener.unregister()

        config.close()
        path.toFile().delete()
    }
}
