package de.vulpescloud.node.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

class ConfigProvider {

    lateinit var config: NodeConfig
        private set

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        allowComments = true
        allowTrailingComma = true
        ignoreUnknownKeys = true
    }

    fun loadConfig(): Boolean {
        if (Files.exists(Path("config.json"))) {
            config = json.decodeFromString(Files.readString(Path("config.json")))
            return true
        } else {
            val defaultConfig =
                NodeConfig(
                    "cloud",
                    UUID.randomUUID(),
                    6565,
                    "0.0.0.0",
                    "0.0.0.0",
                    MongoConfig("mongodb://localhost:27017/", "vulpescloud", "vc_"),
                    4096,
                    "LOCAL",
                    DockerConfig(),
                    true,
                )

            Files.writeString(Path("config.json"), json.encodeToString(defaultConfig))
            config = defaultConfig

            return false
        }
    }

    fun updateConfig(config: NodeConfig) {
        Files.deleteIfExists(Path("config.json"))
        Files.writeString(Path("config.json"), json.encodeToString(config))
        this.config = config
    }
}
