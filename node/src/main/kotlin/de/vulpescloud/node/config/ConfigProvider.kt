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

    fun loadConfig(): Boolean {
        if (Files.exists(Path("config.json"))) {
            config = json.decodeFromString(Files.readString(Path("config.json")))
            return true
        } else {
            val defaultConfig =
                NodeConfig(
                    "Node-1",
                    UUID.randomUUID(),
                    6565,
                    "0.0.0.0",
                    "0.0.0.0",
                    MongoConfig("mongodb://localhost:27017/", "vulpescloud", "vc_"),
                )

            Files.writeString(Path("config.json"), json.encodeToString(defaultConfig))
            config = defaultConfig

            return false
        }
    }

    fun updateConfig(config: NodeConfig) {
        Files.writeString(Path("config.json"), json.encodeToString(config))
        this.config = config
    }
}
