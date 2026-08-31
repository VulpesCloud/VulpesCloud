/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.config

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
        allowComments = true
        allowTrailingComma = true
        ignoreUnknownKeys = true
    }

    fun loadConfig(): Boolean {
        if (Files.exists(Path("config.json"))) {
            config = json.decodeFromString(Files.readString(Path("config.json")))
            Files.writeString(Path("config.json"), json.encodeToString(config))
            return true
        } else {
            val defaultConfig =
                NodeConfig(
                    "cloud",
                    UUID.randomUUID(),
                    6565,
                    "0.0.0.0",
                    "0.0.0.0",
                    4096,
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
