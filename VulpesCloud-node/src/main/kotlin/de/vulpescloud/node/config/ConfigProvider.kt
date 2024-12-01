package de.vulpescloud.node.config

import com.electronwill.nightconfig.core.file.FileConfig
import de.vulpescloud.api.language.Languages
import java.util.UUID

class ConfigProvider {

    val config = FileConfig.builder("config.json")
        .autosave()
        .build()

    init {
        config.load()
    }

    val name = config.get<String>("name")
    val uuid = UUID.fromString(config.get("uuid"))
    val redis = RedisEndpointData(
        config.get("redis.user"),
        config.get("redis.hostname"),
        config.get("redis.port"),
        config.get("redis.password")
    )
    val mysql = MySQLEndpointData(
        config.get("mysql.user"),
        config.get("mysql.password"),
        config.get("mysql.database"),
        config.get("mysql.host"),
        config.get("mysql.port"),
        config.get("mysql.ssl")
    )
    val language = Languages.valueOf(config.get("language"))
    val ranFirstSetup = config.get<Boolean>("ranFirstSetup")
}