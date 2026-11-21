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
import org.vulpesstudios.vulpescloud.api.rollout.RolloutProgress
import org.vulpesstudios.vulpescloud.node.db.RolloutRepository

class RolloutStorage {
    private val mutex = Mutex()

    fun get(rolloutId: String): RolloutProgress? {
        return RolloutRepository.get(rolloutId)
    }

    fun getAll(): List<RolloutProgress> {
        return RolloutRepository.getAll()
    }

    fun getActive(): List<RolloutProgress> {
        return getAll().filter { it.status.isActive }
    }

    fun getByTask(taskName: String): List<RolloutProgress> {
        return getAll().filter { it.taskName.equals(taskName, ignoreCase = true) }
    }

    fun getActiveByTask(taskName: String): RolloutProgress? {
        return getAll().firstOrNull {
            it.taskName.equals(taskName, ignoreCase = true) && it.status.isActive
        }
    }

    fun save(progress: RolloutProgress) {
        RolloutRepository.save(progress)
    }

    fun create(progress: RolloutProgress) {
        check(RolloutRepository.save(progress, createOnly = true)) {
            "Rollout ${progress.rolloutId} already exists"
        }
    }

    fun delete(rolloutId: String) {
        RolloutRepository.delete(rolloutId)
    }

    suspend fun update(
        rolloutId: String,
        @Suppress("UNUSED_PARAMETER") maxRetries: Int = 5,
        transform: (RolloutProgress) -> RolloutProgress,
    ): RolloutProgress? {
        mutex.withLock {
            val current = get(rolloutId) ?: return null
            return transform(current).also { save(it) }
        }
    }
}
