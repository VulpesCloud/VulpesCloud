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

package org.vulpesstudios.vulpescloud.node.cluster

import org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.db.NodeSnapshotRepository

object ClusterHelper {

    fun getLocalNodeSnapshot(): NodeSnapshot {
        return NodeSnapshotRepository.get(Node.instance.configProvider.config.nodeName)
            ?: run {
                NodeSnapshotUpdater.generateSnapshot()
            }
    }

    fun getAllNodeSnapshots(): List<NodeSnapshot> {
        return NodeSnapshotRepository.getAll()
    }
}
