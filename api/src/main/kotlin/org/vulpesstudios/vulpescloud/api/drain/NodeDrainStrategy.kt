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

import build.buf.gen.vulpescloud.draining.v1.NodeDrainStrategy as ProtoNodeDrainStrategy

enum class NodeDrainStrategy {
    /** Stop each service once it is empty (or the deadline has passed). */
    PASSIVE,

    /** Stop all services on the node right away. */
    IMMEDIATE;

    fun toDefinition(): ProtoNodeDrainStrategy =
        when (this) {
            PASSIVE -> ProtoNodeDrainStrategy.NODE_DRAIN_STRATEGY_PASSIVE
            IMMEDIATE -> ProtoNodeDrainStrategy.NODE_DRAIN_STRATEGY_IMMEDIATE
        }

    companion object {
        fun fromDefinition(proto: ProtoNodeDrainStrategy): NodeDrainStrategy =
            when (proto) {
                ProtoNodeDrainStrategy.NODE_DRAIN_STRATEGY_IMMEDIATE -> IMMEDIATE
                ProtoNodeDrainStrategy.NODE_DRAIN_STRATEGY_PASSIVE,
                ProtoNodeDrainStrategy.NODE_DRAIN_STRATEGY_UNSPECIFIED,
                ProtoNodeDrainStrategy.UNRECOGNIZED -> PASSIVE
            }
    }
}
