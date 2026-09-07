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

fun Service.isDraining(): Boolean =
    metadata[RolloutMetadata.KEY_DRAINING]?.equals("true", ignoreCase = true) == true

fun Service.rolloutId(): String? =
    metadata[RolloutMetadata.KEY_ROLLOUT_ID]

fun Service.rolloutGeneration(): String? =
    metadata[RolloutMetadata.KEY_ROLLOUT_GENERATION]

fun Service.withRolloutMetadata(rolloutId: String, generation: String? = null): Service {
    val newMeta = metadata.toMutableMap()
    newMeta[RolloutMetadata.KEY_ROLLOUT_ID] = rolloutId
    if (generation != null) {
        newMeta[RolloutMetadata.KEY_ROLLOUT_GENERATION] = generation
        // Only relevant for freshly started replacements - this is what checkReadinessTimeout
        // measures against, since Service.startTime is never actually populated anywhere in the
        // codebase (always epoch 0) and can't be used for this.
        newMeta[RolloutMetadata.KEY_BATCH_STARTED_AT] = System.currentTimeMillis().toString()
    }
    return this.copy(metadata = newMeta)
}

fun Service.withDraining(draining: Boolean = true): Service {
    val newMeta = metadata.toMutableMap()
    if (draining) {
        newMeta[RolloutMetadata.KEY_DRAINING] = "true"
        newMeta[RolloutMetadata.KEY_DRAINING_SINCE] = System.currentTimeMillis().toString()
    } else {
        newMeta.remove(RolloutMetadata.KEY_DRAINING)
        newMeta.remove(RolloutMetadata.KEY_DRAINING_SINCE)
    }
    return this.copy(metadata = newMeta)
}

/** Epoch millis timestamp of when this service was marked draining, if any. */
fun Service.drainingSince(): Long? = metadata[RolloutMetadata.KEY_DRAINING_SINCE]?.toLongOrNull()

/** Epoch millis timestamp of when this service was tagged as a rollout batch replacement, if any. */
fun Service.rolloutBatchStartedAt(): Long? =
    metadata[RolloutMetadata.KEY_BATCH_STARTED_AT]?.toLongOrNull()

/** Removes all rollout-related metadata keys from this service (used by orphan cleanup). */
fun Service.withoutRolloutMetadata(): Service {
    val newMeta = metadata.toMutableMap()
    newMeta.remove(RolloutMetadata.KEY_DRAINING)
    newMeta.remove(RolloutMetadata.KEY_DRAINING_SINCE)
    newMeta.remove(RolloutMetadata.KEY_ROLLOUT_ID)
    newMeta.remove(RolloutMetadata.KEY_ROLLOUT_GENERATION)
    newMeta.remove(RolloutMetadata.KEY_BATCH_STARTED_AT)
    return this.copy(metadata = newMeta)
}

fun ServiceDefinition.isDraining(): Boolean =
    metadataMap[RolloutMetadata.KEY_DRAINING]?.equals("true", ignoreCase = true) == true

fun ServiceDefinition.rolloutId(): String? =
    metadataMap[RolloutMetadata.KEY_ROLLOUT_ID]

fun ServiceDefinition.rolloutGeneration(): String? =
    metadataMap[RolloutMetadata.KEY_ROLLOUT_GENERATION]

object RolloutMetadata {
    const val KEY_DRAINING = "draining"
    const val KEY_ROLLOUT_ID = "rollout_id"
    const val KEY_ROLLOUT_GENERATION = "rollout_generation"
    const val KEY_TASK_ROLLOUT_ID = "rollout_id"
    const val KEY_DRAINING_SINCE = "draining_since"
    const val KEY_BATCH_STARTED_AT = "rollout_batch_started_at"
    /** Marks a service as a freshly started rollout replacement (vs. an old, pre-existing one). */
    const val GENERATION_NEW = "new"
}
