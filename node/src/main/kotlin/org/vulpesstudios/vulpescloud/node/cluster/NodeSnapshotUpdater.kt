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

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.vulpesstudios.vulpescloud.api.cluster.*
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import java.lang.management.ManagementFactory
import kotlin.time.Duration.Companion.seconds

object NodeSnapshotUpdater {

    private var job: Job? = null
    private val osMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    private val nodesDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("nodeSnapshots")
    }

    fun start() {
        job = NodeCoroutineScope.launch {
            while (true) {
                updateLocalNodeSnapshot()
                delay(5.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun generateSnapshot(): NodeSnapshot {
        val config = Node.instance.configProvider.config
        val nodeServices = Node.instance.nodeServices
        val serviceSnapshots = Node.instance.nodeServiceSnapshots

        val totalSystemMemory = osMXBean.totalMemorySize / 1024 / 1024
        val freeSystemMemory = osMXBean.freeMemorySize / 1024 / 1024
        val usedSystemMemory = totalSystemMemory - freeSystemMemory

        val systemSnapshot = SystemSnapshot(
            cpu = CpuSnapshot(
                cores = osMXBean.availableProcessors,
                usage = osMXBean.cpuLoad,
            ),
            memory = MemorySnapshot(
                totalMemory = totalSystemMemory,
                usedMemory = usedSystemMemory,
                availableMemory = freeSystemMemory,
            ),
        )

        val memoryLimit = config.maxMemory.toLong()
        val memoryReserved = nodeServices.sumOf { it.service.task.maxMemory }
        val memoryUsed = serviceSnapshots
            .filter { snapshot -> nodeServices.any { it.service.uuid.toString() == snapshot.uuid } }
            .sumOf { it.heapUsageMemory / 1024 / 1024 }
        val memoryAvailable = (memoryLimit - memoryReserved).coerceAtLeast(0L)

        val serviceSnapshot = NodeServiceSnapshot(
            count = nodeServices.size,
            memoryLimit = memoryLimit,
            memoryUsed = memoryUsed,
            memoryReserved = memoryReserved,
            memoryAvailable = memoryAvailable,
        )

        return NodeSnapshot(
            name = config.nodeName,
            uuid = config.uuid,
            state = Node.instance.clusterProvider.currentState,
            playersOnNode = Node.instance.nodeProxyPlayers.values.sumOf { it.size }.toLong(),
            timestamp = System.currentTimeMillis(),
            startupTimestamp = System.getProperty("startup")?.toLongOrNull() ?: 0L,
            system = systemSnapshot,
            services = serviceSnapshot,
            attributes = Node.instance.clusterProvider.currentAttributes,
        )
    }

    suspend fun updateLocalNodeSnapshot() {
        val snapshot =
            generateSnapshot()

        nodesDatabase.upsert(Node.instance.configProvider.config.nodeName, Json.encodeToJsonElement(snapshot))
    }
}
