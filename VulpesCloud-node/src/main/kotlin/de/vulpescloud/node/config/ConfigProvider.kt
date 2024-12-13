package de.vulpescloud.node.config

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.json.JsonFormat
import de.vulpescloud.api.language.Languages
import java.util.UUID

class ConfigProvider {

    val config: FileConfig = FileConfig.builder("config.json", JsonFormat.fancyInstance())
        .autosave()
        .sync()
        .build()

    init {
        config.load()

        if (config.get<String>("name") == null) {
            config.set<String>("redis.user", "")
            config.set<String>("redis.hostname", "")
            config.set<Int>("redis.port", 0)
            config.set<String>("redis.password", "")

            config.set<String>("mysql.user", "")
            config.set<String>("mysql.password", "")
            config.set<String>("mysql.database", "")
            config.set<String>("mysql.host", "")
            config.set<Int>("mysql.port", 0)
            config.set<Boolean>("mysql.ssl", false)

            config.set<String>("name", "FIST_SETUP")
            config.set<Boolean>("ranFirstSetup", false)
            config.set<String>("language", Languages.en_US.name)

            config.set<String>("uuid", "00000000-0000-0000-0000-000000000000")
        }
    }
    val hostname = "0.0.0.0" //todo Add Config entry and make it pull this from there!

    val name: String = config.get("name")
    val uuid: UUID = UUID.fromString(config.get("uuid"))
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
    val ranFirstSetup: Boolean = config.get("ranFirstSetup")
}