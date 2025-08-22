package de.vulpescloud.node.config

import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

class ConfigProvider {

    lateinit var config: NodeConfig
        private set

    val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun loadConfig() {
        if (Files.exists(Path("config.json"))) {
            config = json.decodeFromString(Files.readString(Path("config.json")))
        } else {
            val defaultConfig =
                NodeConfig(
                    "unset",
                    UUID.randomUUID(),
                    6565,
                    "0.0.0.0",
                    "0.0.0.0",
                    "en_US",
                    MongoConfig("mongodb://localhost:27017/", "vulpescloud", "vc_"),
                )

            Files.writeString(Path("config.json"), json.encodeToString(defaultConfig))
            config = defaultConfig
        }
    }
}
