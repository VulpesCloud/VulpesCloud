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

package org.vulpesstudios.vulpescloud.node.services

import build.buf.gen.vulpescloud.services.v1.getAllServicesRequest
import build.buf.gen.vulpescloud.tasks.v1.getAllTasksRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import kotlin.time.Duration.Companion.seconds

object ServiceScheduler {

    private var job: Job? = null
    private val logger = LoggerFactory.getLogger("ServiceScheduler")

    fun start() {
        job =
            NodeCoroutineScope.launch {
                while (true) {
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

                    tasks.forEach { task ->
                        logger.debug("Checking task ${task.name}")
                        val currentServiceCount = services.count { it.task.name == task.name }
                        if (!task.autoStart) return@forEach
                        if (task.preferredNode != Node.instance.configProvider.config.nodeName)
                            return@forEach
                        if (task.minOnlineServices <= currentServiceCount) return@forEach
                        logger.info("Starting service on Task ${task.name}")

                        val factory =
                            Node.instance.serviceFactoryProvider.findServiceFactory(
                                task.serviceFactoryName
                            )
                                ?: throw IllegalArgumentException(
                                    "Unable to find ServiceFactory ${task.serviceFactoryName}"
                                )
                        factory.prepareService(task).start()
                    }

                    delay(5.seconds)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
