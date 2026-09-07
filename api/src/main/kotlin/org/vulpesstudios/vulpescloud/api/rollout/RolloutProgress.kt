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

package org.vulpesstudios.vulpescloud.api.rollout

import com.google.protobuf.Timestamp
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.TimestampSerializer
import build.buf.gen.vulpescloud.rollout.v1.RolloutProgress as ProtoRolloutProgress

@Serializable
data class RolloutProgress(
    val rolloutId: String,
    val taskName: String,
    val status: RolloutStatus,
    val options: RolloutOptions,
    val totalServicesToReplace: Int = 0,
    val servicesStarted: Int = 0,
    val servicesReady: Int = 0,
    val servicesStopped: Int = 0,
    val pendingStopServiceNames: List<String> = emptyList(),
    val newServiceNames: List<String> = emptyList(),
    @Serializable(TimestampSerializer::class) val startedAt: Timestamp? = null,
    @Serializable(TimestampSerializer::class) val completedAt: Timestamp? = null,
    val failureReason: String = "",
    val currentBatchNumber: Int = 0,
    val totalBatches: Int = 0,
) {
    fun toDefinition(): ProtoRolloutProgress {
        val builder = ProtoRolloutProgress.newBuilder()
            .setRolloutId(rolloutId)
            .setTaskName(taskName)
            .setStatus(status.toDefinition())
            .setOptions(options.toDefinition())
            .setTotalServicesToReplace(totalServicesToReplace)
            .setServicesStarted(servicesStarted)
            .setServicesReady(servicesReady)
            .setServicesStopped(servicesStopped)
            .addAllPendingStopServiceNames(pendingStopServiceNames)
            .addAllNewServiceNames(newServiceNames)
            .setFailureReason(failureReason)
            .setCurrentBatchNumber(currentBatchNumber)
            .setTotalBatches(totalBatches)

        startedAt?.let { builder.setStartedAt(it) }
        completedAt?.let { builder.setCompletedAt(it) }

        return builder.build()
    }

    companion object {
        fun fromDefinition(definition: ProtoRolloutProgress): RolloutProgress {
            return RolloutProgress(
                rolloutId = definition.rolloutId,
                taskName = definition.taskName,
                status = RolloutStatus.fromDefinition(definition.status),
                options = RolloutOptions.fromDefinition(definition.options),
                totalServicesToReplace = definition.totalServicesToReplace,
                servicesStarted = definition.servicesStarted,
                servicesReady = definition.servicesReady,
                servicesStopped = definition.servicesStopped,
                pendingStopServiceNames = definition.pendingStopServiceNamesList,
                newServiceNames = definition.newServiceNamesList,
                startedAt = if (definition.hasStartedAt()) definition.startedAt else null,
                completedAt = if (definition.hasCompletedAt()) definition.completedAt else null,
                failureReason = definition.failureReason,
                currentBatchNumber = definition.currentBatchNumber,
                totalBatches = definition.totalBatches,
            )
        }
    }
}
