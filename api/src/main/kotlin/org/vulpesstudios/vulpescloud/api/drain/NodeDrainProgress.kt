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

package org.vulpesstudios.vulpescloud.api.drain

import com.google.protobuf.Timestamp
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.TimestampSerializer
import build.buf.gen.vulpescloud.draining.v1.NodeDrainProgress as ProtoNodeDrainProgress

/**
 * Persisted state of a node drain. A node has at most one active drain, so it is keyed by [nodeName].
 */
@Serializable
data class NodeDrainProgress(
    val nodeName: String,
    val strategy: NodeDrainStrategy = NodeDrainStrategy.PASSIVE,
    val status: NodeDrainStatus = NodeDrainStatus.PENDING,
    @Serializable(TimestampSerializer::class) val startedAt: Timestamp? = null,
    @Serializable(TimestampSerializer::class) val deadline: Timestamp? = null,
    @Serializable(TimestampSerializer::class) val completedAt: Timestamp? = null,
    val playerThreshold: Int = 0,
    val servicesTotal: Int = 0,
    val servicesStopped: Int = 0,
    val servicesRemaining: Int = 0,
    val playersRemaining: Int = 0,
    val failureReason: String? = null,
    /** Internal lifecycle reason; intentionally not part of the public protobuf payload. */
    val drainReason: String? = null,
) {
    fun toDefinition(): ProtoNodeDrainProgress {
        val builder =
            ProtoNodeDrainProgress.newBuilder()
                .setNodeName(nodeName)
                .setStrategy(strategy.toDefinition())
                .setStatus(status.toDefinition())
                .setPlayerThreshold(playerThreshold)
                .setServicesTotal(servicesTotal)
                .setServicesStopped(servicesStopped)
                .setServicesRemaining(servicesRemaining)
                .setPlayersRemaining(playersRemaining)

        startedAt?.let { builder.setStartedAt(it) }
        deadline?.let { builder.setDeadline(it) }
        completedAt?.let { builder.setCompletedAt(it) }
        failureReason?.let { builder.setFailureReason(it) }

        return builder.build()
    }

    companion object {
        fun fromDefinition(definition: ProtoNodeDrainProgress): NodeDrainProgress =
            NodeDrainProgress(
                nodeName = definition.nodeName,
                strategy = NodeDrainStrategy.fromDefinition(definition.strategy),
                status = NodeDrainStatus.fromDefinition(definition.status),
                startedAt = if (definition.hasStartedAt()) definition.startedAt else null,
                deadline = if (definition.hasDeadline()) definition.deadline else null,
                completedAt = if (definition.hasCompletedAt()) definition.completedAt else null,
                playerThreshold = definition.playerThreshold,
                servicesTotal = definition.servicesTotal,
                servicesStopped = definition.servicesStopped,
                servicesRemaining = definition.servicesRemaining,
                playersRemaining = definition.playersRemaining,
                failureReason = if (definition.hasFailureReason()) definition.failureReason else null,
            )
    }
}
