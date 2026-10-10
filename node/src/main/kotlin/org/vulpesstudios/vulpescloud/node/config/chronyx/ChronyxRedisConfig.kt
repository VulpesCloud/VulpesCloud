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

package org.vulpesstudios.vulpescloud.node.config.chronyx

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.exists

@Serializable
data class ChronyxRedisConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val database: Int,
) {
    companion object {
        fun read(): ChronyxRedisConfig {
            val path = Path("local/database/redis.chronyx.json")
            if (!path.exists()) {
                val config = ChronyxRedisConfig("127.0.0.1", 6379, "default", "", 0)
                path.toFile().writeText(Json.encodeToString(config))
                return config
            } else {
                return Json.decodeFromString(path.toFile().readText())
            }
        }
    }
}
