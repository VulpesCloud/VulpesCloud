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

package org.vulpesstudios.vulpescloud.api.services

import build.buf.gen.vulpescloud.services.v1.ServiceDefinition
import build.buf.gen.vulpescloud.services.v1.ServiceState
import com.google.protobuf.Timestamp
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.TimestampSerializer
import org.vulpesstudios.vulpescloud.api.serializer.UUIDSerializer
import org.vulpesstudios.vulpescloud.api.tasks.Task
import java.util.*

@Serializable
data class Service(
    val task: Task,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val orderedId: Int,
    val port: Int,
    val node: String,
    val playerCount: Int,
    @Serializable(TimestampSerializer::class) val startTime: Timestamp,
    val state: ServiceStates,
    val hostname: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun name(): String = task.name + "-" + orderedId

    fun toDefinition(): ServiceDefinition =
        ServiceDefinition.newBuilder()
            .setTask(task.toDefinition())
            .setUuid(uuid.toString())
            .setOrderedId(orderedId)
            .setPort(port)
            .setNode(node)
            .setPlayerCount(playerCount)
            .setStartTime(startTime)
            .setState(
                when (state) {
                    ServiceStates.UNKNOWN -> ServiceState.SERVICE_STATE_UNSPECIFIED
                    ServiceStates.PREPARED -> ServiceState.SERVICE_STATE_PREPARED
                    ServiceStates.STARTING -> ServiceState.SERVICE_STATE_STARTING
                    ServiceStates.RUNNING -> ServiceState.SERVICE_STATE_RUNNING
                    ServiceStates.STOPPED -> ServiceState.SERVICE_STATE_STOPPED
                }
            )
            .setHostname(hostname)
            .putAllMetadata(metadata)
            .build()

    companion object {
        fun fromDefinition(definition: ServiceDefinition): Service {
            return Service(
                Task.fromDefinition(definition.task),
                UUID.fromString(definition.uuid),
                definition.orderedId,
                definition.port,
                definition.node,
                definition.playerCount,
                definition.startTime,
                when (definition.state) {
                    ServiceState.SERVICE_STATE_UNSPECIFIED -> ServiceStates.UNKNOWN
                    ServiceState.SERVICE_STATE_PREPARED -> ServiceStates.PREPARED
                    ServiceState.SERVICE_STATE_STARTING -> ServiceStates.STARTING
                    ServiceState.SERVICE_STATE_RUNNING -> ServiceStates.RUNNING
                    ServiceState.SERVICE_STATE_STOPPED -> ServiceStates.STOPPED
                    ServiceState.UNRECOGNIZED -> ServiceStates.UNKNOWN
                },
                definition.hostname,
                definition.metadataMap,
            )
        }
    }
}
