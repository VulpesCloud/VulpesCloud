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

class VulpesChronyxStore : ChronyxStore {
    private val database by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("chronyxLeases")
    }
    private val json = Json

    private fun key(task: String) = "lease:$task"

    private suspend fun read(task: String) =
        database.get(key(task))?.let { json.decodeFromJsonElement(ChronyxLease.serializer(), it) }

    override suspend fun acquire(
        taskName: String,
        hostId: String,
        maxGlobalInstances: Int,
        maxHostInstances: Int,
        leaseDurationMillis: Long,
    ): String? {
        while (true) {
            val raw = database.get(key(taskName))
            val current = raw?.let { json.decodeFromJsonElement(ChronyxLease.serializer(), it) }
            if (current != null && current.expires > System.currentTimeMillis()) return null
            val next =
                ChronyxLease(
                    taskName,
                    hostId,
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis(),
                    System.currentTimeMillis() + leaseDurationMillis,
                )
            if (database.compareAndSet(key(taskName), raw, json.encodeToJsonElement(next)))
                return next.token
        }
    }

    override suspend fun renew(
        taskName: String,
        hostId: String,
        token: String,
        leaseDurationMillis: Long,
    ): Boolean {
        val raw = database.get(key(taskName)) ?: return false
        val current = json.decodeFromJsonElement(ChronyxLease.serializer(), raw)
        return !(current.host != hostId ||
            current.token != token ||
            current.expires <= System.currentTimeMillis()) &&
            database.compareAndSet(
                key(taskName),
                raw,
                json.encodeToJsonElement(
                    current.copy(expires = System.currentTimeMillis() + leaseDurationMillis)
                ),
            )
    }

    override suspend fun release(taskName: String, hostId: String, token: String): Boolean {
        val raw = database.get(key(taskName)) ?: return false
        val current = json.decodeFromJsonElement(ChronyxLease.serializer(), raw)
        return !(current.host != hostId || current.token != token) &&
            database.compareAndSet(
                key(taskName),
                raw,
                json.encodeToJsonElement(current.copy(expires = 0)),
            )
    }

    override suspend fun getGlobalCount(taskName: String): Int =
        if ((read(taskName)?.expires ?: 0) > System.currentTimeMillis()) 1 else 0

    override suspend fun getHostCount(taskName: String, hostId: String): Int =
        if (
            read(taskName)?.let { it.host == hostId && it.expires > System.currentTimeMillis() } ==
                true
        )
            1
        else 0

    override suspend fun getLockInfo(taskName: String): LockInfo? =
        read(taskName)
            ?.takeIf { it.expires > System.currentTimeMillis() }
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
