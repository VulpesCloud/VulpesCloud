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

import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.UUIDSerializer
import java.util.*

@Serializable
data class NodeConfig(
    val nodeName: String,
    @Serializable(with = UUIDSerializer::class) val uuid: UUID,
    val grpcPort: Int,
    val grpcHost: String,
    val serviceBindAdress: String,
    val maxMemory: Int = 1024, // MB
    val docker: DockerConfig,
    val useModernForwarding: Boolean,
    val auth: AuthConfig = AuthConfig("changeme", "changeme2", true),
    val databaseType: String = "sqlite",
    val testing: TestingConfig = TestingConfig(),
    val serviceStartDelayMillis: Int = 1000,
)
