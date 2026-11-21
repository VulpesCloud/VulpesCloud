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

package org.vulpesstudios.vulpescloud.node.drain

import org.vulpesstudios.vulpescloud.api.drain.NodeDrainProgress
import org.vulpesstudios.vulpescloud.node.db.NodeDrainRepository

/**
 * Persists [NodeDrainProgress] keyed by node name (a node has at most one drain record; a new drain
 * replaces the previous, finished one).
 */
class NodeDrainStorage {
    fun get(nodeName: String): NodeDrainProgress? {
        return NodeDrainRepository.get(nodeName)
    }

    fun getAll(): List<NodeDrainProgress> = NodeDrainRepository.getAll()

    fun getActive(): List<NodeDrainProgress> = getAll().filter { it.status.isActive }

    fun getActive(nodeName: String): NodeDrainProgress? =
        get(nodeName)?.takeIf { it.status.isActive }

    fun save(progress: NodeDrainProgress) {
        NodeDrainRepository.save(progress)
    }

    fun delete(nodeName: String) {
        NodeDrainRepository.delete(nodeName)
    }
}
