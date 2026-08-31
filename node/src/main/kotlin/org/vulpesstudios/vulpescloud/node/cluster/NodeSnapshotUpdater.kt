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
import org.vulpesstudios.vulpescloud.api.cluster.NodeSnapshot
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

    private fun usedMemory(): Long {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
            1024 /
            1024)
    }

    fun generateSnapshot(): NodeSnapshot {
        return NodeSnapshot(
            Node.instance.configProvider.config.nodeName,
            Node.instance.configProvider.config.uuid,
            Node.instance.clusterProvider.currentState,
            usedMemory(),
            -1L,
            -1L,
            osMXBean.cpuLoad,
            0,
            System.currentTimeMillis(),
            Node.instance.clusterProvider.currentAttributes,
            Node.instance.nodeServices.size.toLong()
        )
    }

    suspend fun updateLocalNodeSnapshot() {
        val snapshot =
            generateSnapshot()

        nodesDatabase.upsert(Node.instance.configProvider.config.nodeName, Json.encodeToJsonElement(snapshot))
    }
}
