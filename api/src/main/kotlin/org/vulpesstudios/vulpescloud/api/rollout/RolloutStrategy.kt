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

import build.buf.gen.vulpescloud.rollout.v1.RolloutStrategy as ProtoRolloutStrategy

enum class RolloutStrategy {
    ROLLING_REPLACE,
    PASSIVE_DRAIN,
    IMMEDIATE;

    fun toDefinition(): ProtoRolloutStrategy {
        return when (this) {
            ROLLING_REPLACE -> ProtoRolloutStrategy.ROLLOUT_STRATEGY_ROLLING_REPLACE
            PASSIVE_DRAIN -> ProtoRolloutStrategy.ROLLOUT_STRATEGY_PASSIVE_DRAIN
            IMMEDIATE -> ProtoRolloutStrategy.ROLLOUT_STRATEGY_IMMEDIATE
        }
    }

    companion object {
        fun fromDefinition(proto: ProtoRolloutStrategy): RolloutStrategy {
            return when (proto) {
                ProtoRolloutStrategy.ROLLOUT_STRATEGY_ROLLING_REPLACE -> ROLLING_REPLACE
                ProtoRolloutStrategy.ROLLOUT_STRATEGY_PASSIVE_DRAIN -> PASSIVE_DRAIN
                ProtoRolloutStrategy.ROLLOUT_STRATEGY_IMMEDIATE -> IMMEDIATE
                ProtoRolloutStrategy.ROLLOUT_STRATEGY_UNSPECIFIED,
                ProtoRolloutStrategy.UNRECOGNIZED -> ROLLING_REPLACE
            }
        }

        fun fromStringOrNull(name: String?): RolloutStrategy? {
            if (name == null) return null
            return when (name.trim().lowercase()) {
                "rolling", "rolling_replace", "rollingreplace", "rolling-replace" -> ROLLING_REPLACE
                "passive", "passive_drain", "passivedrain", "passive-drain", "drain" -> PASSIVE_DRAIN
                "immediate", "force" -> IMMEDIATE
                else -> entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            }
        }
    }
}

fun ProtoRolloutStrategy.toRolloutStrategy(): RolloutStrategy = RolloutStrategy.fromDefinition(this)
