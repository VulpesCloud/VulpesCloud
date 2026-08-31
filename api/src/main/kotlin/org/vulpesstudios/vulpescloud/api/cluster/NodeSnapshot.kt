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

package org.vulpesstudios.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.cluster.v2.NodeSnapshot
import build.buf.gen.vulpescloud.cluster.v2.NodeState
import build.buf.gen.vulpescloud.cluster.v2.nodeSnapshot
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.UUIDSerializer
import java.util.*

@Serializable
data class NodeSnapshot(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val state: org.vulpesstudios.vulpescloud.api.cluster.NodeState,
    val nodeProcessUsedMemory: Long,
    val servicesUsedMemory: Long,
    val servicesMaxMemory: Long,
    val systemCpuUsage: Double,
    val onlinePlayers: Long,
    val timestamp: Long,
    val attributes: Map<String, String>,
    val services: Long,
) {

    fun toDefinition(): NodeSnapshot {
        return nodeSnapshot {
            this.name = this@NodeSnapshot.name
            this.uuid = this@NodeSnapshot.uuid.toString()
            this.servicesUsedMemory = this@NodeSnapshot.servicesUsedMemory
            this.onlinePlayers = this@NodeSnapshot.onlinePlayers
            this.timestamp = this@NodeSnapshot.timestamp
            this.systemCpuUsage = this@NodeSnapshot.systemCpuUsage
            this.nodeProccessUsedMemory = this@NodeSnapshot.nodeProcessUsedMemory
            this.servicesMaxMemory = this@NodeSnapshot.servicesMaxMemory
            this.attributes.putAll(attributes)
            this.services = this@NodeSnapshot.services
            this.state =
                when (this@NodeSnapshot.state) {
                    org.vulpesstudios.vulpescloud.api.cluster.NodeState.DRAINING -> NodeState.NODE_STATES_DRAINING
                    org.vulpesstudios.vulpescloud.api.cluster.NodeState.OFFLINE ->
                        NodeState.NODE_STATES_OFFLINE_UNSPECIFIED
                    org.vulpesstudios.vulpescloud.api.cluster.NodeState.ONLINE -> NodeState.NODE_STATES_ONLINE
                    org.vulpesstudios.vulpescloud.api.cluster.NodeState.BOOTING -> NodeState.NODE_STATES_BOOTING
                }
        }
    }

    companion object {
        fun fromDefinition(definition: NodeSnapshot): org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot {
            return NodeSnapshot(
                definition.name,
                UUID.fromString(definition.uuid),
                when (definition.state) {
                    NodeState.NODE_STATES_ONLINE -> org.vulpesstudios.vulpescloud.api.cluster.NodeState.ONLINE
                    NodeState.NODE_STATES_BOOTING -> org.vulpesstudios.vulpescloud.api.cluster.NodeState.BOOTING
                    NodeState.NODE_STATES_OFFLINE_UNSPECIFIED ->
                        org.vulpesstudios.vulpescloud.api.cluster.NodeState.OFFLINE
                    NodeState.NODE_STATES_DRAINING -> org.vulpesstudios.vulpescloud.api.cluster.NodeState.DRAINING
                    else -> org.vulpesstudios.vulpescloud.api.cluster.NodeState.OFFLINE
                },
                definition.nodeProccessUsedMemory,
                definition.servicesMaxMemory,
                definition.servicesUsedMemory,
                definition.systemCpuUsage,
                definition.onlinePlayers,
                definition.timestamp,
                definition.attributesMap,
                definition.services,
            )
        }
    }
}
