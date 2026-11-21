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

package org.vulpesstudios.vulpescloud.node.templates

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.templates.TemplateJobProgress
import org.vulpesstudios.vulpescloud.node.db.TemplateJobLockRepository
import org.vulpesstudios.vulpescloud.node.db.TemplateJobRepository
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shared relational job records. Database locks serialize checkpoint updates; conditional row
 * updates claim workers.
 */
@Suppress("UNUSED")
class TemplateJobStorage {
    private val mutex = Mutex()

    fun get(jobId: String): TemplateJobProgress? = getRecord(jobId)?.progress

    fun getRecord(jobId: String): TemplateJobRecord? = TemplateJobRepository.get(jobId)

    fun getAllRecords(): List<TemplateJobRecord> = TemplateJobRepository.getAll()

    fun getAll(): List<TemplateJobProgress> = getAllRecords().map { it.progress }

    fun getActive(): List<TemplateJobProgress> =
        getAllRecords().map { it.progress }.filter { it.status.isActive }

    fun create(record: TemplateJobRecord) {
        check(TemplateJobRepository.save(record, createOnly = true)) {
            "Template job ${record.progress.jobId} already exists"
        }
    }

    suspend fun saveRecord(record: TemplateJobRecord) {
        updateRecord(record.progress.jobId) { record }
    }

    suspend fun save(progress: TemplateJobProgress) {
        updateRecord(progress.jobId) { current ->
            (current ?: TemplateJobRecord(progress)).copy(progress = progress)
        }
    }

    suspend fun updateRecord(
        jobId: String,
        maxRetries: Int = 20,
        transform: (TemplateJobRecord?) -> TemplateJobRecord,
    ): TemplateJobRecord? {
        return mutex.withLock {
            val key = "job-update:$jobId"
            val owner = "update:${java.util.UUID.randomUUID()}"
            val acquiredAt = System.currentTimeMillis()
            var acquired = false
            for (attempt in 0 until maxRetries) {
                if (
                    TemplateJobLockRepository.tryAcquire(
                        key,
                        TemplateJobLock(
                            owner,
                            acquiredAt,
                            "Template job checkpoint",
                            acquiredAt + 30_000,
                        ),
                        acquiredAt,
                    )
                ) {
                    acquired = true
                    break
                }
                delay(10.milliseconds)
            }
            if (!acquired) {
                logger.warn("Could not acquire persistence lock for template job {}", jobId)
                return@withLock null
            }
            try {
                val updated = transform(TemplateJobRepository.get(jobId))
                TemplateJobRepository.save(updated)
                updated
            } finally {
                TemplateJobLockRepository.deleteOwned(key, owner)
            }
        }
    }

    fun claim(jobId: String, nodeName: String, now: Long, leaseMillis: Long): TemplateJobRecord? {
        return TemplateJobRepository.claim(jobId, nodeName, now, leaseMillis)
    }

    fun delete(jobId: String) {
        TemplateJobRepository.delete(jobId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateJobStorage::class.java)
    }
}

/** Durable lock slots are keyed by template ID or by a reservation for a not-yet-created target. */
class TemplateJobLockStorage {
    private val mutex = Mutex()

    fun get(key: String): TemplateJobLock? {
        val lock = TemplateJobLockRepository.get(key) ?: return null
        if (lock.leaseUntil == 0L || lock.leaseUntil > System.currentTimeMillis()) return lock
        TemplateJobLockRepository.deleteOwned(key, lock.jobId)
        return null
    }

    fun getAll(): List<Pair<String, TemplateJobLock>> =
        TemplateJobLockRepository.getAll().filter { (_, it) ->
            it.jobId.isNotBlank() &&
                (it.leaseUntil == 0L || it.leaseUntil > System.currentTimeMillis())
        }

    suspend fun acquire(
        keys: Set<String>,
        jobId: String,
        description: String,
        leaseUntil: Long = 0,
    ): Boolean {
        return mutex.withLock {
            val acquired = mutableListOf<String>()
            val now = System.currentTimeMillis()
            for (key in keys.sorted()) {
                if (
                    !TemplateJobLockRepository.tryAcquire(
                        key,
                        TemplateJobLock(jobId, now, description, leaseUntil),
                        now,
                    )
                ) {
                    acquired.forEach { TemplateJobLockRepository.deleteOwned(it, jobId) }
                    return@withLock false
                }
                acquired += key
            }
            true
        }
    }

    suspend fun release(keys: Set<String>, jobId: String) {
        mutex.withLock { keys.forEach { key -> TemplateJobLockRepository.deleteOwned(key, jobId) } }
    }

    suspend fun renew(keys: Set<String>, jobId: String, leaseUntil: Long): Boolean {
        return mutex.withLock {
            var renewed = true
            keys.forEach { key ->
                if (!TemplateJobLockRepository.renewOwned(key, jobId, leaseUntil)) renewed = false
            }
            renewed
        }
    }
}
