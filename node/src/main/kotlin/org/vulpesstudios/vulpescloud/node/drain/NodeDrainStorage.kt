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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.drain.NodeDrainProgress
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.db.Database

/**
 * Persists [NodeDrainProgress] keyed by node name (a node has at most one drain record; a new drain
 * replaces the previous, finished one).
 */
class NodeDrainStorage {
    private val database: Database by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase(DATABASE_NAME)
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(nodeName: String): NodeDrainProgress? {
        val element = database.get(nodeName) ?: return null
        return try {
            json.decodeFromJsonElement<NodeDrainProgress>(element)
        } catch (e: Exception) {
            logger.error("Failed to decode node drain progress for node $nodeName", e)
            null
        }
    }

    suspend fun getAll(): List<NodeDrainProgress> =
        database.getAll().mapNotNull { element ->
            try {
                json.decodeFromJsonElement<NodeDrainProgress>(element)
            } catch (e: Exception) {
                logger.error("Failed to decode node drain element in getAll", e)
                null
            }
        }

    suspend fun getActive(): List<NodeDrainProgress> = getAll().filter { it.status.isActive }

    suspend fun getActive(nodeName: String): NodeDrainProgress? =
        get(nodeName)?.takeIf { it.status.isActive }

    suspend fun save(progress: NodeDrainProgress) {
        database.upsert(progress.nodeName, json.encodeToJsonElement(progress))
    }

    suspend fun delete(nodeName: String) {
        database.delete(nodeName)
    }

    companion object {
        const val DATABASE_NAME = "vc_node_drains"
        private val logger = LoggerFactory.getLogger(NodeDrainStorage::class.java)
    }
}
