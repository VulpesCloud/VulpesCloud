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

package org.vulpesstudios.vulpescloud.node.coordination

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.vulpesstudios.chronyx.ChronyxStore
import org.vulpesstudios.chronyx.LockInfo
import org.vulpesstudios.vulpescloud.node.Node
import java.time.Instant
import java.util.*

@Serializable
private data class ChronyxLease(
    val task: String,
    val host: String,
    val token: String,
    val acquired: Long,
    val expires: Long,
)

@Serializable private data class ChronyxLeaseSet(val leases: List<ChronyxLease>)

class VulpesChronyxStore : ChronyxStore {
    private val database by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("chronyxLeases")
    }
    private val json = Json

    private fun key(task: String) = "lease:$task"

    private fun decode(raw: JsonElement?): List<ChronyxLease> =
        when (raw) {
            null -> emptyList()
            is JsonObject if "leases" in raw ->
                json.decodeFromJsonElement(ChronyxLeaseSet.serializer(), raw).leases

            else -> listOf(json.decodeFromJsonElement(ChronyxLease.serializer(), raw))
        }

    private fun encode(leases: List<ChronyxLease>): JsonElement =
        json.encodeToJsonElement(ChronyxLeaseSet(leases))

    private suspend fun active(task: String): List<ChronyxLease> {
        val now = System.currentTimeMillis()
        return decode(database.get(key(task))).filter { it.expires > now }
    }

    override suspend fun acquire(
        taskName: String,
        hostId: String,
        maxGlobalInstances: Int,
        maxHostInstances: Int,
        leaseDurationMillis: Long,
    ): String? {
        while (true) {
            val raw = database.get(key(taskName))
            val now = System.currentTimeMillis()
            // Expired leases are dropped whenever we write, so the document doesn't grow forever.
            val active = decode(raw).filter { it.expires > now }

            if (active.size >= maxGlobalInstances) return null
            if (active.count { it.host == hostId } >= maxHostInstances) return null

            val lease =
                ChronyxLease(
                    task = taskName,
                    host = hostId,
                    token = UUID.randomUUID().toString(),
                    acquired = now,
                    expires = now + leaseDurationMillis,
                )
            if (database.compareAndSet(key(taskName), raw, encode(active + lease))) {
                return lease.token
            }
        }
    }

    override suspend fun renew(
        taskName: String,
        hostId: String,
        token: String,
        leaseDurationMillis: Long,
    ): Boolean {
        while (true) {
            val raw = database.get(key(taskName)) ?: return false
            val now = System.currentTimeMillis()
            val active = decode(raw).filter { it.expires > now }

            val current =
                active.firstOrNull { it.host == hostId && it.token == token } ?: return false
            val updated = active.map {
                if (it.token == token) current.copy(expires = now + leaseDurationMillis) else it
            }
            if (database.compareAndSet(key(taskName), raw, encode(updated))) return true
        }
    }

    override suspend fun release(taskName: String, hostId: String, token: String): Boolean {
        while (true) {
            val raw = database.get(key(taskName)) ?: return false
            val now = System.currentTimeMillis()
            val leases = decode(raw)

            if (leases.none { it.host == hostId && it.token == token }) return false
            // Remove the released lease and prune anything expired while we're at it.
            val remaining = leases.filter { it.expires > now && it.token != token }
            if (database.compareAndSet(key(taskName), raw, encode(remaining))) return true
        }
    }

    override suspend fun getGlobalCount(taskName: String): Int = active(taskName).size

    override suspend fun getHostCount(taskName: String, hostId: String): Int =
        active(taskName).count { it.host == hostId }

    override suspend fun getLockInfo(taskName: String): LockInfo? =
        active(taskName)
            .minByOrNull { it.acquired }
            ?.let {
                LockInfo(
                    it.task,
                    it.host,
                    it.token,
                    Instant.ofEpochMilli(it.acquired),
                    Instant.ofEpochMilli(it.expires),
                )
            }
}
