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

package org.vulpesstudios.vulpescloud.api.cluster

import build.buf.gen.vulpescloud.cluster.v2.NodeState

enum class NodeState {

    OFFLINE,
    BOOTING,
    ONLINE,
    DRAINING;

    fun toNodeStates(): NodeState {
        return when (this) {
            OFFLINE -> NodeState.NODE_STATES_OFFLINE_UNSPECIFIED
            BOOTING -> NodeState.NODE_STATES_BOOTING
            ONLINE -> NodeState.NODE_STATES_ONLINE
            DRAINING -> NodeState.NODE_STATES_DRAINING
        }
    }
}
