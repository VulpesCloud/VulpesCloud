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
import org.vulpesstudios.vulpescloud.api.tasks.Task

object RolloutConfigResolver {

    fun resolve(
        flagOptions: RolloutOptions? = null,
        task: Task? = null,
        globalConfig: RolloutGlobalConfig = RolloutGlobalConfig(),
    ): RolloutOptions {
        return resolve(
            flagOptions = flagOptions,
            taskAttributes = task?.attributes ?: emptyMap(),
            globalConfig = globalConfig,
        )
    }

    fun resolve(
        flagOptions: RolloutOptions? = null,
        taskAttributes: Map<String, String>,
        globalConfig: RolloutGlobalConfig = RolloutGlobalConfig(),
    ): RolloutOptions {
        return resolve(
            strategy = flagOptions?.strategy,
            batchSize = flagOptions?.batchSize,
            drainPlayerThreshold = flagOptions?.drainPlayerThreshold,
            drainTimeout = flagOptions?.drainTimeout,
            readinessTimeout = flagOptions?.readinessTimeout,
            bypassMaxServiceCount = flagOptions?.bypassMaxServiceCount,
            fallbackRemainingPlayers = flagOptions?.fallbackRemainingPlayers,
            taskAttributes = taskAttributes,
            globalConfig = globalConfig,
        )
    }

    fun resolve(
        strategy: RolloutStrategy? = null,
        batchSize: Int? = null,
        drainPlayerThreshold: Int? = null,
        drainTimeout: Duration? = null,
        readinessTimeout: Duration? = null,
        bypassMaxServiceCount: Boolean? = null,
        fallbackRemainingPlayers: Boolean? = null,
        task: Task? = null,
        taskAttributes: Map<String, String> = task?.attributes ?: emptyMap(),
        globalConfig: RolloutGlobalConfig = RolloutGlobalConfig(),
    ): RolloutOptions {
        val effectiveStrategy = strategy
            ?: taskAttributes["rollout.strategy"]?.let { RolloutStrategy.fromStringOrNull(it) }
            ?: taskAttributes["rollout.default_strategy"]?.let { RolloutStrategy.fromStringOrNull(it) }
            ?: taskAttributes["rollout_strategy"]?.let { RolloutStrategy.fromStringOrNull(it) }
            ?: globalConfig.defaultStrategy

        val effectiveBatchSize = (batchSize?.takeIf { it > 0 }
            ?: taskAttributes["rollout.batch_size"]?.toIntOrNull()
            ?: taskAttributes["rollout.batchSize"]?.toIntOrNull()
            ?: taskAttributes["rollout.batch"]?.toIntOrNull()
            ?: taskAttributes["rollout.default_batch_size"]?.toIntOrNull()
            ?: globalConfig.defaultBatchSize).coerceAtLeast(1)

        val effectiveDrainPlayerThreshold = drainPlayerThreshold
            ?: taskAttributes["rollout.drain_player_threshold"]?.toIntOrNull()
            ?: taskAttributes["rollout.drainPlayerThreshold"]?.toIntOrNull()
            ?: taskAttributes["rollout.drain_threshold"]?.toIntOrNull()
            ?: taskAttributes["rollout.default_drain_threshold"]?.toIntOrNull()
            ?: globalConfig.defaultDrainPlayerThreshold

        val effectiveDrainTimeout = drainTimeout
            ?: taskAttributes["rollout.drain_timeout"]?.let { parseDuration(it) }
            ?: taskAttributes["rollout.drainTimeout"]?.let { parseDuration(it) }
            ?: taskAttributes["rollout.default_drain_timeout"]?.let { parseDuration(it) }
            ?: globalConfig.defaultDrainTimeout

        val effectiveReadinessTimeout = readinessTimeout
            ?: taskAttributes["rollout.readiness_timeout"]?.let { parseDuration(it) }
            ?: taskAttributes["rollout.readinessTimeout"]?.let { parseDuration(it) }
            ?: taskAttributes["rollout.default_readiness_timeout"]?.let { parseDuration(it) }
            ?: globalConfig.defaultReadinessTimeout

        val effectiveBypassMaxServiceCount = bypassMaxServiceCount
            ?: taskAttributes["rollout.bypass_max_service_count"]?.toBooleanStrictOrNull()
            ?: taskAttributes["rollout.bypassMaxServiceCount"]?.toBooleanStrictOrNull()
            ?: taskAttributes["rollout.force"]?.toBooleanStrictOrNull()
            ?: taskAttributes["rollout.default_bypass_max_service_count"]?.toBooleanStrictOrNull()
            ?: globalConfig.defaultBypassMaxServiceCount

        val effectiveFallbackRemainingPlayers = fallbackRemainingPlayers
            ?: taskAttributes["rollout.fallback_remaining_players"]?.toBooleanStrictOrNull()
            ?: taskAttributes["rollout.fallbackRemainingPlayers"]?.toBooleanStrictOrNull()
            ?: taskAttributes["rollout.default_fallback_players"]?.toBooleanStrictOrNull()
            ?: globalConfig.defaultFallbackRemainingPlayers

        return RolloutOptions(
            strategy = effectiveStrategy,
            batchSize = effectiveBatchSize,
            drainPlayerThreshold = effectiveDrainPlayerThreshold,
            drainTimeout = effectiveDrainTimeout,
            readinessTimeout = effectiveReadinessTimeout,
            bypassMaxServiceCount = effectiveBypassMaxServiceCount,
            fallbackRemainingPlayers = effectiveFallbackRemainingPlayers,
        )
    }

    fun parseDuration(value: String): Duration? {
        val trimmed = value.trim().lowercase()
        return try {
            when {
                trimmed.endsWith("ms") -> {
                    val ms = trimmed.removeSuffix("ms").toLong()
                    Duration.newBuilder().setSeconds(ms / 1000).setNanos(((ms % 1000) * 1_000_000).toInt()).build()
                }
                trimmed.endsWith("s") -> {
                    val s = trimmed.removeSuffix("s").toLong()
                    Duration.newBuilder().setSeconds(s).build()
                }
                trimmed.endsWith("m") -> {
                    val m = trimmed.removeSuffix("m").toLong()
                    Duration.newBuilder().setSeconds(m * 60).build()
                }
                trimmed.endsWith("h") -> {
                    val h = trimmed.removeSuffix("h").toLong()
                    Duration.newBuilder().setSeconds(h * 3600).build()
                }
                else -> {
                    val s = trimmed.toLong()
                    Duration.newBuilder().setSeconds(s).build()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
