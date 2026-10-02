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

import build.buf.gen.vulpescloud.draining.v1.NodeDrainStatus as ProtoNodeDrainStatus

enum class NodeDrainStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    FAILED;

    val isActive: Boolean
        get() = this == PENDING || this == IN_PROGRESS

    val isFinished: Boolean
        get() = !isActive

    fun toDefinition(): ProtoNodeDrainStatus =
        when (this) {
            PENDING -> ProtoNodeDrainStatus.NODE_DRAIN_STATUS_PENDING
            IN_PROGRESS -> ProtoNodeDrainStatus.NODE_DRAIN_STATUS_IN_PROGRESS
            COMPLETED -> ProtoNodeDrainStatus.NODE_DRAIN_STATUS_COMPLETED
            CANCELLED -> ProtoNodeDrainStatus.NODE_DRAIN_STATUS_CANCELLED
            FAILED -> ProtoNodeDrainStatus.NODE_DRAIN_STATUS_FAILED
        }

    companion object {
        fun fromDefinition(proto: ProtoNodeDrainStatus): NodeDrainStatus =
            when (proto) {
                ProtoNodeDrainStatus.NODE_DRAIN_STATUS_IN_PROGRESS -> IN_PROGRESS
                ProtoNodeDrainStatus.NODE_DRAIN_STATUS_COMPLETED -> COMPLETED
                ProtoNodeDrainStatus.NODE_DRAIN_STATUS_CANCELLED -> CANCELLED
                ProtoNodeDrainStatus.NODE_DRAIN_STATUS_FAILED -> FAILED
                ProtoNodeDrainStatus.NODE_DRAIN_STATUS_PENDING,
                ProtoNodeDrainStatus.NODE_DRAIN_STATUS_UNSPECIFIED,
                ProtoNodeDrainStatus.UNRECOGNIZED -> PENDING
            }
    }
}
