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

import com.google.protobuf.Duration
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.serializer.DurationSerializer
import build.buf.gen.vulpescloud.rollout.v1.RolloutOptions as ProtoRolloutOptions

@Serializable
data class RolloutOptions(
    val strategy: RolloutStrategy = RolloutStrategy.ROLLING_REPLACE,
    val batchSize: Int = 1,
    val drainPlayerThreshold: Int? = null,
    @Serializable(DurationSerializer::class) val drainTimeout: Duration? = null,
    @Serializable(DurationSerializer::class) val readinessTimeout: Duration = Duration.newBuilder().setSeconds(60).build(),
    val bypassMaxServiceCount: Boolean = false,
    val fallbackRemainingPlayers: Boolean = true,
) {
    fun toDefinition(): ProtoRolloutOptions {
        val builder = ProtoRolloutOptions.newBuilder()
            .setStrategy(strategy.toDefinition())
            .setBatchSize(batchSize)
            .setReadinessTimeout(readinessTimeout)
            .setBypassMaxServiceCount(bypassMaxServiceCount)
            .setFallbackRemainingPlayers(fallbackRemainingPlayers)

        drainPlayerThreshold?.let { builder.setDrainPlayerThreshold(it) }
        drainTimeout?.let { builder.setDrainTimeout(it) }

        return builder.build()
    }

    companion object {
        fun fromDefinition(definition: ProtoRolloutOptions): RolloutOptions {
            return RolloutOptions(
                strategy = RolloutStrategy.fromDefinition(definition.strategy),
                batchSize = if (definition.batchSize > 0) definition.batchSize else 1,
                drainPlayerThreshold = if (definition.hasDrainPlayerThreshold()) definition.drainPlayerThreshold else null,
                drainTimeout = if (definition.hasDrainTimeout()) definition.drainTimeout else null,
                readinessTimeout = if (definition.hasReadinessTimeout()) definition.readinessTimeout else Duration.newBuilder().setSeconds(60).build(),
                bypassMaxServiceCount = definition.bypassMaxServiceCount,
                fallbackRemainingPlayers = definition.fallbackRemainingPlayers,
            )
        }
    }
}
