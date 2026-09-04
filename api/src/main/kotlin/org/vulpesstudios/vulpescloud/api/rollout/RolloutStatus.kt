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

import build.buf.gen.vulpescloud.rollout.v1.RolloutStatus as ProtoRolloutStatus

enum class RolloutStatus {
    PENDING,
    IN_PROGRESS,
    DRAINING,
    COMPLETED,
    FAILED,
    CANCELLED;

    fun toDefinition(): ProtoRolloutStatus {
        return when (this) {
            PENDING -> ProtoRolloutStatus.ROLLOUT_STATUS_PENDING
            IN_PROGRESS -> ProtoRolloutStatus.ROLLOUT_STATUS_IN_PROGRESS
            DRAINING -> ProtoRolloutStatus.ROLLOUT_STATUS_DRAINING
            COMPLETED -> ProtoRolloutStatus.ROLLOUT_STATUS_COMPLETED
            FAILED -> ProtoRolloutStatus.ROLLOUT_STATUS_FAILED
            CANCELLED -> ProtoRolloutStatus.ROLLOUT_STATUS_CANCELLED
        }
    }

    val isActive: Boolean
        get() = this == PENDING || this == IN_PROGRESS || this == DRAINING

    val isFinished: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED

    companion object {
        fun fromDefinition(proto: ProtoRolloutStatus): RolloutStatus {
            return when (proto) {
                ProtoRolloutStatus.ROLLOUT_STATUS_PENDING -> PENDING
                ProtoRolloutStatus.ROLLOUT_STATUS_IN_PROGRESS -> IN_PROGRESS
                ProtoRolloutStatus.ROLLOUT_STATUS_DRAINING -> DRAINING
                ProtoRolloutStatus.ROLLOUT_STATUS_COMPLETED -> COMPLETED
                ProtoRolloutStatus.ROLLOUT_STATUS_FAILED -> FAILED
                ProtoRolloutStatus.ROLLOUT_STATUS_CANCELLED -> CANCELLED
                ProtoRolloutStatus.ROLLOUT_STATUS_UNSPECIFIED,
                ProtoRolloutStatus.UNRECOGNIZED -> PENDING
            }
        }
    }
}

fun ProtoRolloutStatus.toRolloutStatus(): RolloutStatus = RolloutStatus.fromDefinition(this)
