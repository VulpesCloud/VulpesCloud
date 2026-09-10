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

package org.vulpesstudios.vulpescloud.node.virtualconfig

import build.buf.gen.vulpescloud.virtualconfig.v1.createVirtualConfigRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.Node

object VirtualConfigDebugHelper {

    private val logger = LoggerFactory.getLogger("VirtualConfigDebugHelper")

    suspend fun createDebugConfig() {
        logger.info("Creating debug config...")
        Node.instance.localGrpcClient.virtualConfigAPI.createVirtualConfig(
            createVirtualConfigRequest {
                this.name = "debug_config"
                this.config = Json.encodeToString(DebugConfig(true, AnotherDebugConfig("test")))
            }
        )
        logger.info("Debug config created!")
    }

    suspend fun updateDebugConfig() {
        logger.info("Updating debug config...")
        Node.instance.virtualConfigProvider.updateCustomConfig(
            "debug_config",
            DebugConfig(false, AnotherDebugConfig("test2")),
        )
        logger.info("Debug config updated!")
    }


}

@Serializable data class AnotherDebugConfig(val testing: String)

@Serializable data class DebugConfig(val enabled: Boolean, val anotherConfig: AnotherDebugConfig)
