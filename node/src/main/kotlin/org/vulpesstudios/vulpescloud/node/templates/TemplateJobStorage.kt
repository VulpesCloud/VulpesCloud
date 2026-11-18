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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.templates.TemplateJobProgress
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.db.Database

/** Shared database-backed job records. compareAndSet protects worker claims and checkpoints. */
class TemplateJobStorage {
    private val database: Database by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase(DATABASE_NAME)
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(jobId: String): TemplateJobProgress? = getRecord(jobId)?.progress

    suspend fun getRecord(jobId: String): TemplateJobRecord? = decode(jobId, database.get(jobId))

    suspend fun getAllRecords(): List<TemplateJobRecord> =
        database.getAllEntries().mapNotNull { (key, value) -> decode(key, value) }

    suspend fun getAll(): List<TemplateJobProgress> = getAllRecords().map { it.progress }

    suspend fun getActive(): List<TemplateJobProgress> =
        getAllRecords().map { it.progress }.filter { it.status.isActive }

    suspend fun create(record: TemplateJobRecord) {
        database.insert(record.progress.jobId, json.encodeToJsonElement(record))
    }

    suspend fun saveRecord(record: TemplateJobRecord) {
        database.upsert(record.progress.jobId, json.encodeToJsonElement(record))
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
        repeat(maxRetries) {
            val old = database.get(jobId)
            val current = old?.let { runCatching { json.decodeFromJsonElement<TemplateJobRecord>(it) }.getOrNull() }
            if (old != null && current == null) {
                logger.error("Failed to decode template job record {}", jobId)
                return null
            }
            val updated = transform(current)
            if (database.compareAndSet(jobId, old, json.encodeToJsonElement(updated))) return updated
        }
        logger.warn("Could not update template job {} after {} compare-and-set attempts", jobId, maxRetries)
        return null
    }

    suspend fun claim(jobId: String, nodeName: String, now: Long, leaseMillis: Long): TemplateJobRecord? {
        repeat(20) {
            val old = database.get(jobId) ?: return null
            val current = runCatching { json.decodeFromJsonElement<TemplateJobRecord>(old) }.getOrNull()
                ?: return null
            if (!current.progress.status.isActive || current.workerLeaseUntil > now) return null
            val claimed = current.copy(
                workerNodeName = nodeName,
                workerLeaseUntil = now + leaseMillis,
                workerToken = java.util.UUID.randomUUID().toString(),
            )
            if (database.compareAndSet(jobId, old, json.encodeToJsonElement(claimed))) return claimed
        }
        return null
    }

    suspend fun delete(jobId: String) {
        database.delete(jobId)
    }

    private fun decode(key: String, value: JsonElement?): TemplateJobRecord? {
        if (value == null) return null
        return try {
            json.decodeFromJsonElement<TemplateJobRecord>(value)
        } catch (e: Exception) {
            logger.error("Failed to decode template job record {}", key, e)
            null
        }
    }

    companion object {
        const val DATABASE_NAME = "vc_template_jobs"
        private val logger = LoggerFactory.getLogger(TemplateJobStorage::class.java)
    }
}

/** Durable lock slots are keyed by template ID or by a reservation for a not-yet-created target. */
class TemplateJobLockStorage {
    private val database: Database by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase(DATABASE_NAME)
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(key: String): TemplateJobLock? {
        repeat(20) {
            val old = database.get(key) ?: return null
            val lock = decode(old)?.takeIf { it.jobId.isNotBlank() } ?: return null
            if (lock.leaseUntil == 0L || lock.leaseUntil > System.currentTimeMillis()) return lock
            if (database.compareAndSet(key, old, json.encodeToJsonElement(TemplateJobLock("", 0, "")))) return null
        }
        val current = database.get(key)?.let(::decode) ?: return null
        return current.takeIf { it.jobId.isNotBlank() && (it.leaseUntil == 0L || it.leaseUntil > System.currentTimeMillis()) }
    }

    suspend fun getAll(): List<Pair<String, TemplateJobLock>> =
        database.getAllEntries().mapNotNull { (key, value) ->
            decode(value)?.takeIf {
                it.jobId.isNotBlank() && (it.leaseUntil == 0L || it.leaseUntil > System.currentTimeMillis())
            }?.let { key to it }
        }

    suspend fun acquire(
        keys: Set<String>,
        jobId: String,
        description: String,
        leaseUntil: Long = 0,
    ): Boolean {
        val acquiredNow = mutableListOf<String>()
        for (key in keys.sorted()) {
            var acquired = false
            var newlyAcquired = false
            repeat(20) {
                if (acquired) return@repeat
                val old = database.get(key)
                val current = decode(old)
                if (current?.jobId == jobId) {
                    acquired = true
                } else if (current != null && current.jobId.isNotBlank() &&
                    (current.leaseUntil == 0L || current.leaseUntil > System.currentTimeMillis())
                ) {
                    return@repeat
                } else {
                    val lock = TemplateJobLock(jobId, System.currentTimeMillis(), description, leaseUntil)
                    acquired = database.compareAndSet(key, old, json.encodeToJsonElement(lock))
                    newlyAcquired = acquired
                }
            }
            if (!acquired) {
                release(acquiredNow.toSet(), jobId)
                return false
            }
            if (newlyAcquired) acquiredNow += key
        }
        return true
    }

    suspend fun release(keys: Set<String>, jobId: String) {
        for (key in keys) {
            repeat(20) {
                val old = database.get(key) ?: return@repeat
                val current = decode(old) ?: return@repeat
                if (current.jobId != jobId) return@repeat
                val free = TemplateJobLock("", 0, "", 0)
                if (database.compareAndSet(key, old, json.encodeToJsonElement(free))) return@repeat
            }
        }
    }

    suspend fun renew(keys: Set<String>, jobId: String, leaseUntil: Long): Boolean {
        var renewed = true
        for (key in keys) {
            var updated = false
            repeat(20) {
                if (updated) return@repeat
                val old = database.get(key) ?: return@repeat
                val current = decode(old) ?: return@repeat
                if (current.jobId != jobId) return@repeat
                updated = database.compareAndSet(
                    key,
                    old,
                    json.encodeToJsonElement(current.copy(leaseUntil = leaseUntil)),
                )
            }
            if (!updated) renewed = false
        }
        return renewed
    }

    private fun decode(value: JsonElement?): TemplateJobLock? =
        if (value == null) null else runCatching { json.decodeFromJsonElement<TemplateJobLock>(value) }.getOrNull()

    companion object {
        const val DATABASE_NAME = "vc_template_locks"
    }
}
