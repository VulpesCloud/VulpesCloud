package de.vulpescloud.node.config

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.json.JsonFormat
import de.vulpescloud.api.language.Languages
import org.slf4j.LoggerFactory
import java.util.UUID

class ConfigProvider {
    private val logger = LoggerFactory.getLogger(ConfigProvider::class.java)

    fun <T:Any> getEntry(entry: String, default: T, addIfMissing: Boolean = true): T {
        if (config.contains(entry)) {
            return config.get(entry)
        } else {
            if (addIfMissing) {
                config.set<T>(entry, default)
                config.save()
                return default
            }
            logger.warn("CONFIG: Trying to get entry $entry but it won't be added to the config file!")
            return default
        }
    }

    val config: FileConfig = FileConfig.builder("config.json", JsonFormat.fancyInstance())
        .autosave()
        .sync()
        .build()

    init {
        config.load()
    }
    val hostname = getEntry("hostname", "0.0.0.0")

    val name = getEntry("name", "UNSET")
    val uuid: UUID = getEntry("uuid", UUID.fromString("00000000-0000-0000-0000-000000000000"))
    val redis = RedisEndpointData(
        getEntry("redis.user", "default"),
        getEntry("redis.host", "127.0.0.1"),
        getEntry("redis.port", 6379),
        getEntry("redis.password", "")
    )
    val mysql = MySQLEndpointData(
        getEntry("mysql.user", "root"),
        getEntry("mysql.password", ""),
        getEntry("mysql.database", "vulpescloud"),
        getEntry("mysql.host", "127.0.0.1"),
        getEntry("mysql.port",3306),
        getEntry("mysql.ssl", false)
    )
    val language = Languages.valueOf(getEntry("language", "en_US"))
    val ranFirstSetup: Boolean = getEntry("ranFirstSetup", false)
    val serviceStopTimeout: Int = getEntry("service_stop_timeout", 15)
}