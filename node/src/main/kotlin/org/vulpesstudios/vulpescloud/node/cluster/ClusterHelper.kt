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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot
import org.vulpesstudios.vulpescloud.node.Node

object ClusterHelper {

    private val snapshotDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodeSnapshots")
    }
    private val json = Json

    suspend fun getLocalNodeSnapshot(): NodeSnapshot {
        val jsonElement = snapshotDatabase.get(Node.instance.configProvider.config.nodeName)
        return if (jsonElement != null) {
            json.decodeFromJsonElement(jsonElement)
        } else {
            NodeSnapshotUpdater.generateSnapshot()
        }
    }

    suspend fun getAllNodeSnapshots(): List<NodeSnapshot> {
        return snapshotDatabase.getAll().map { json.decodeFromJsonElement(it) }
    }
}
