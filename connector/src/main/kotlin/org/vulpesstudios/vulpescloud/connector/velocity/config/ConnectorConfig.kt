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

package org.vulpesstudios.vulpescloud.connector.velocity.config

import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.bridge.BridgeAPI

@Serializable
data class ConnectorConfig(
    val hubCommandConfig: HubCommandConfig = HubCommandConfig(),
    val disconnectNoAvailableServerMessage: String =
        "<red>There is no available server for you to connect to!</red>",
    val prefix: String = "<gray>[<gradient:#EE660A:#D9BC40>VulpesCloud</gradient>]</gray> ",
)

@Serializable data class HubCommandConfig(val enabled: Boolean = true)

private val bridgeAPI = BridgeAPI.createCoroutineAPI()

suspend fun getConfig(): ConnectorConfig {
    return bridgeAPI
        .getVirtualConfigAPI()
        .getCustomConfigObject("vc_connector", ConnectorConfig.serializer(), false)
        ?: throw IllegalStateException("ConnectorConfig not found!")
}
