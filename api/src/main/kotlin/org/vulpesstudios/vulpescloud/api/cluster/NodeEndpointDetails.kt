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

import build.buf.gen.vulpescloud.cluster.v2.Node
import build.buf.gen.vulpescloud.cluster.v2.node
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.UUIDSerializer
import java.util.*

@Serializable
data class NodeEndpointDetails(
    val name: String,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val host: String,
    val port: Int,
) {
    fun toDefinition(): Node {
        return node {
            this.name = this@NodeEndpointDetails.name
            this.uuid = this@NodeEndpointDetails.uuid.toString()
            this.grpcAddress = this@NodeEndpointDetails.host
            this.grpcPort = this@NodeEndpointDetails.port
        }
    }

    companion object {
        fun fromDefinition(definition: Node): NodeEndpointDetails {
            return NodeEndpointDetails(
                definition.name,
                UUID.fromString(definition.uuid),
                definition.grpcAddress,
                definition.grpcPort,
            )
        }
    }
}
