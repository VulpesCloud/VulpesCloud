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

import build.buf.gen.vulpescloud.cluster.v2.ServiceSnapshot
import build.buf.gen.vulpescloud.cluster.v2.serviceSnapshot
import kotlinx.serialization.Serializable

@Serializable
data class NodeServiceSnapshot(
    val count: Int,
    val memoryLimit: Long,
    val memoryUsed: Long,
    val memoryReserved: Long,
    val memoryAvailable: Long,
) {

    fun toDefinition(): ServiceSnapshot {
        return serviceSnapshot {
            this.count = this@NodeServiceSnapshot.count
            this.memoryLimit = this@NodeServiceSnapshot.memoryLimit
            this.memoryUsed = this@NodeServiceSnapshot.memoryUsed
            this.memoryReserved = this@NodeServiceSnapshot.memoryReserved
            this.memoryAvailable = this@NodeServiceSnapshot.memoryAvailable
        }
    }

    companion object {
        fun fromDefinition(definition: ServiceSnapshot): NodeServiceSnapshot {
            return NodeServiceSnapshot(
                definition.count,
                definition.memoryLimit,
                definition.memoryUsed,
                definition.memoryReserved,
                definition.memoryAvailable,
            )
        }
    }
}
