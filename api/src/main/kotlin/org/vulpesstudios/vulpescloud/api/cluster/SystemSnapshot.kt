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

import build.buf.gen.vulpescloud.cluster.v2.systemSnapshot
import kotlinx.serialization.Serializable

@Serializable
data class SystemSnapshot(
    val cpu: CpuSnapshot,
    val memory: MemorySnapshot,
) {

    fun toDefinition(): build.buf.gen.vulpescloud.cluster.v2.SystemSnapshot {
        return systemSnapshot {
            this.cpu = this@SystemSnapshot.cpu.toDefinition()
            this.memory = this@SystemSnapshot.memory.toDefinition()
        }
    }

    companion object {
        fun fromDefinition(definition: build.buf.gen.vulpescloud.cluster.v2.SystemSnapshot): SystemSnapshot {
            return SystemSnapshot(
                CpuSnapshot.fromDefinition(definition.cpu),
                MemorySnapshot.fromDefinition(definition.memory),
            )
        }
    }
}
