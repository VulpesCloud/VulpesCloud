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

@Serializable
data class RolloutGlobalConfig(
    val defaultStrategy: RolloutStrategy = RolloutStrategy.ROLLING_REPLACE,
    val defaultBatchSize: Int = 1,
    val defaultDrainPlayerThreshold: Int = 0,
    @Serializable(DurationSerializer::class) val defaultDrainTimeout: Duration = Duration.newBuilder().setSeconds(60).build(),
    @Serializable(DurationSerializer::class) val defaultReadinessTimeout: Duration = Duration.newBuilder().setSeconds(60).build(),
    val defaultBypassMaxServiceCount: Boolean = false,
    val defaultFallbackRemainingPlayers: Boolean = true,
) {
    companion object {
        const val VIRTUAL_CONFIG_NAME = "vc_rollout_config"

        const val KEY_STRATEGY = "rollout.default_strategy"
        const val KEY_BATCH_SIZE = "rollout.default_batch_size"
        const val KEY_DRAIN_THRESHOLD = "rollout.default_drain_threshold"
        const val KEY_DRAIN_TIMEOUT = "rollout.default_drain_timeout"
        const val KEY_READINESS_TIMEOUT = "rollout.default_readiness_timeout"
        const val KEY_BYPASS_MAX_SERVICE_COUNT = "rollout.default_bypass_max_service_count"
        const val KEY_FALLBACK_PLAYERS = "rollout.default_fallback_players"
    }
}
