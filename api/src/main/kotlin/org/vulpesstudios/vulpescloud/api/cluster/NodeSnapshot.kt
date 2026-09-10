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

import build.buf.gen.vulpescloud.cluster.v2.NodeState
import build.buf.gen.vulpescloud.cluster.v2.nodeSnapshot
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.UUIDSerializer
import java.util.*
import build.buf.gen.vulpescloud.cluster.v2.NodeSnapshot as ProtoNodeSnapshot

@Serializable
data class NodeSnapshot(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,

    val state: org.vulpesstudios.vulpescloud.api.cluster.NodeState,

    val playersOnNode: Long,

    val timestamp: Long,
    val startupTimestamp: Long,

    val system: SystemSnapshot,
    val services: NodeServiceSnapshot,

    val attributes: Map<String, String>,
) {

    fun toDefinition(): ProtoNodeSnapshot {
        return nodeSnapshot {
            this.name = this@NodeSnapshot.name
            this.uuid = this@NodeSnapshot.uuid.toString()

            this.state = this@NodeSnapshot.state.toNodeStates()

            this.playersOnNode = this@NodeSnapshot.playersOnNode

            this.timestamp = this@NodeSnapshot.timestamp
            this.startupTimestamp = this@NodeSnapshot.startupTimestamp

            this.system = this@NodeSnapshot.system.toDefinition()
            this.services = this@NodeSnapshot.services.toDefinition()

            this.attributes.putAll(this@NodeSnapshot.attributes)
        }
    }

    companion object {
        fun fromDefinition(definition: ProtoNodeSnapshot): NodeSnapshot {
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
                definition.playersOnNode,
                definition.timestamp,
                definition.startupTimestamp,
                SystemSnapshot.fromDefinition(definition.system),
                NodeServiceSnapshot.fromDefinition(definition.services),
                definition.attributesMap,
            )
        }
    }
}
