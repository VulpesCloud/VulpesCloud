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

import build.buf.gen.vulpescloud.services.v1.getAllServicesRequest
import build.buf.gen.vulpescloud.tasks.v1.PrepareServiceOnTaskRequest
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import org.slf4j.LoggerFactory
import org.vulpesstudios.chronyx.Chronyx
import org.vulpesstudios.chronyx.StoreTaskManager
import org.vulpesstudios.chronyx.TaskManager
import org.vulpesstudios.chronyx.chronyx
import org.vulpesstudios.vulpescloud.api.cluster.NodeState
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.cluster.ClusterHelper
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.mariadb.MariaDBDatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.mongo.MongoDBDatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.sqlite.SQLiteDatabaseProvider
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthClientInterceptor

class ChronyxCoordinator {
    private val logger = LoggerFactory.getLogger("ChronyxClusterCoordinator")
    private lateinit var chronyx: Chronyx

    fun start() {
        val manager = taskManager()
        chronyx = chronyx {
            hostId = Node.instance.configProvider.config.uuid.toString()
            manager(manager)
            leaseDurationMillis = 15_000
            renewIntervalMillis = 5_000
        }
        chronyx.task("service-reconciler", manager.name, "*/5 * * * * *", 1, 1) {
            reconcileServices()
        }

        chronyx.start()
        logger.info("Chronyx cluster coordinator started with {}", manager.name)
    }

    fun stop() {
        if (::chronyx.isInitialized) chronyx.stop()
    }

    private fun taskManager(): TaskManager =
        when (val provider = DatabaseProvider.getMainDatabaseProvider()) {
            is SQLiteDatabaseProvider,
            is MariaDBDatabaseProvider,
            is MongoDBDatabaseProvider -> StoreTaskManager("vulpescloud", VulpesChronyxStore())
            else -> error("Unsupported cluster database provider: ${provider::class.simpleName}")
        }

    private suspend fun reconcileServices() {
        val tasks =
            Node.instance.localGrpcClient.tasksAPI
                .getAllTasks(getAllTasksRequest {})
                .tasksList
                .map { Task.fromDefinition(it) }

        val services =
            Node.instance.localGrpcClient.serviceAPI
                .getAllServices(getAllServicesRequest {})
                .servicesList
                .map { Service.fromDefinition(it) }

        val nodeSnapshots = ClusterHelper.getAllNodeSnapshots()

        tasks.forEach { task ->
            logger.debug("Checking task ${task.name}")
            val currentServiceCount = services.count { it.task.name == task.name }
            if (!task.autoStart) return@forEach
            if (task.minOnlineServices <= currentServiceCount) return@forEach

            val bestNode =
                nodeSnapshots
                    .filter { it.name in task.preferredNodes }
                    .filter { it.state == NodeState.ONLINE }
                    .filter { it.services.memoryAvailable >= task.maxMemory }
                    .maxByOrNull { it.services.memoryAvailable }

            if (bestNode == null) {
                logger.debug(
                    "No eligible node found for task ${task.name}, ignoring this reconciling run"
                )
                return@forEach
            }

            val request =
                PrepareServiceOnTaskRequest.newBuilder()
                    .setTask(task.toDefinition())
                    .setAmount(1)
                    .setMemory(task.maxMemory)
                    .setNodeName(bestNode.name)
                    .setStart(true)
                    .setStartId(1)
                    .build()
            logger.info(
                "Preparing and starting service for task ${task.name} on Node ${bestNode.name}"
            )
            if (bestNode.name == Node.instance.configProvider.config.nodeName) {
                Node.instance.localGrpcClient.tasksAPI.prepareServiceOnTask(request)
            } else {
                Node.instance.clusterProvider.remoteNodes
                    .find { it.endpoint.name == bestNode.name }
                    ?.let {
                        TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(it.channel!!)
                            .withInterceptors(AuthClientInterceptor(Node.instance.secret))
                            .prepareServiceOnTask(request)
                    }
            }
        }
    }
}
