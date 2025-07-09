package de.vulpescloud.node.config

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.json.JsonFormat
import de.vulpescloud.api.lang.Languages
import java.util.*
import org.slf4j.LoggerFactory

class NodeConfig {
    private val logger = LoggerFactory.getLogger(NodeConfig::class.java)

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
                "CONFIG: Trying to get entry $entry but it won't be added to the config file!"
            )
            return default
        }
    }

    val config: FileConfig =
        FileConfig.builder("config.json", JsonFormat.fancyInstance()).autosave().sync().build()

    fun initializeConfig() {
        config.load()
    }

    fun hostname() = getEntry("hostname", "0.0.0.0")

    fun name() = getEntry("name", "UNSET")

    fun uuid() = UUID.fromString(getEntry("uuid", "00000000-0000-0000-0000-000000000000"))

    fun redis() =
        RedisEndpointData(
            getEntry("redis.user", "default"),
            getEntry("redis.host", "127.0.0.1"),
            getEntry("redis.port", 6379),
            getEntry("redis.password", ""),
        )

    fun mysql() =
        MySQLEndpointData(
            getEntry("mysql.user", "root"),
            getEntry("mysql.password", ""),
            getEntry("mysql.database", "vulpescloud"),
            getEntry("mysql.host", "127.0.0.1"),
            getEntry("mysql.port", 3306),
            getEntry("mysql.ssl", false),
        )

    fun language() = Languages.valueOf(getEntry("language", "en_US"))
    fun ranFirstSetup() = getEntry("ranFirstSetup", false)

    fun serviceStopTimeout() = getEntry("service_stop_timeout", 15)

    fun hasBeenMigratedToV2() = getEntry("migrated_to_v2", false)
}
