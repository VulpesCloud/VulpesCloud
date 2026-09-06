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

package org.vulpesstudios.vulpescloud.node.rollout

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.db.Database

class RolloutStorage {
    private val database: Database by lazy { Node.instance.getDatabaseProvider().getOrCreateDatabase(DATABASE_NAME) }
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(rolloutId: String): RolloutProgress? {
        val element = database.get(rolloutId) ?: return null
        return try {
            json.decodeFromJsonElement<RolloutProgress>(element)
        } catch (e: Exception) {
            logger.error("Failed to decode rollout progress for id $rolloutId", e)
            null
        }
    }

    suspend fun getAll(): List<RolloutProgress> {
        return database.getAll().mapNotNull { element ->
            try {
                json.decodeFromJsonElement<RolloutProgress>(element)
            } catch (e: Exception) {
                logger.error("Failed to decode rollout element in getAll", e)
                null
            }
        }
    }

    suspend fun getActive(): List<RolloutProgress> {
        return getAll().filter { it.status.isActive }
    }

    suspend fun getByTask(taskName: String): List<RolloutProgress> {
        return getAll().filter { it.taskName.equals(taskName, ignoreCase = true) }
    }

    suspend fun getActiveByTask(taskName: String): RolloutProgress? {
        return getAll().firstOrNull { it.taskName.equals(taskName, ignoreCase = true) && it.status.isActive }
    }

    suspend fun save(progress: RolloutProgress) {
        val encoded = json.encodeToJsonElement(progress)
        database.upsert(progress.rolloutId, encoded)
    }

    suspend fun create(progress: RolloutProgress) {
        val encoded = json.encodeToJsonElement(progress)
        database.insert(progress.rolloutId, encoded)
    }

    suspend fun delete(rolloutId: String) {
        database.delete(rolloutId)
    }

    suspend fun update(
        rolloutId: String,
        maxRetries: Int = 5,
        transform: (RolloutProgress) -> RolloutProgress
    ): RolloutProgress? {
        mutex.withLock {
            var retries = 0
            while (retries < maxRetries) {
                val currentElement = database.get(rolloutId) ?: return null
                val currentProgress = try {
                    json.decodeFromJsonElement<RolloutProgress>(currentElement)
                } catch (e: Exception) {
                    logger.error("Failed to decode rollout $rolloutId for update", e)
                    return null
                }

                val updatedProgress = transform(currentProgress)
                val newElement = json.encodeToJsonElement(updatedProgress)

                val success = database.compareAndSet(rolloutId, currentElement, newElement)
                if (success) {
                    return updatedProgress
                }
                retries++
            }
            // If compareAndSet failed after retries, fallback to upsert under lock
            val current = get(rolloutId) ?: return null
            val updated = transform(current)
            save(updated)
            return updated
        }
    }

    companion object {
        const val DATABASE_NAME = "vc_rollouts"
        private val logger = LoggerFactory.getLogger(RolloutStorage::class.java)
    }
}
