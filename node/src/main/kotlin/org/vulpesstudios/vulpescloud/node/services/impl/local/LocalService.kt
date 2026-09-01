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

package org.vulpesstudios.vulpescloud.node.services.impl.local

import build.buf.gen.vulpescloud.events.v1.serviceLogEvent
import build.buf.gen.vulpescloud.events.v1.serviceStateChangedEvent
import build.buf.gen.vulpescloud.services.v1.ServiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.api.services.ServiceStates
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import org.vulpesstudios.vulpescloud.node.event.EventsService
import org.vulpesstudios.vulpescloud.node.services.AbstractService
import org.vulpesstudios.vulpescloud.node.utils.MongoUtils
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.seconds

class LocalService(override val service: Service) : AbstractService {

    override fun path(): Path {
        return if (service.task.staticServices) {
            Path.of("local/services/${service.task.name}-${service.orderedId}")
        } else {
            Path.of("temp/services/local/${service.task.name}-${service.orderedId}")
        }
    }

    var processBuilder: ProcessBuilder? = null

    private val logger =
        LoggerFactory.getLogger("LocalService-${service.task.name}-${service.orderedId}")

    var process: Process? = null
    private var processTracking: Thread? = null

    override fun start() {
        logger.info("Starting service ${service.task.name}-${service.orderedId}...")
        if (process != null) {
            logger.warn("Service ${service.task.name}-${service.orderedId} is already running!")
            return
        }

        process = processBuilder?.start()

        Thread {
                process?.inputStream?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        runBlocking {
                            EventsService.publish(
                                serviceLogEvent {
                                    this.service = this@LocalService.service.toDefinition()
                                    this.message = line
                                },
                                true,
                            )
                        }
                    }
                }
            }
            .start()

        this.processTracking = Thread {
            try {
                synchronized(this) { process?.waitFor() }
            } catch (_: InterruptedException) {}
            if (service.state != ServiceStates.STOPPED) {
                if (process != null) {
                    process!!.exitValue()
                }

                this.process = null

                NodeCoroutineScope.launch { MongoUtils.deleteService(service) }
                Node.instance.nodeServices.removeIf { it.service.uuid == service.uuid }

                EventsService.publish(
                    serviceStateChangedEvent {
                        this.service = this@LocalService.service.toDefinition()
                        this.oldState = ServiceStates.RUNNING.toServiceState()
                        this.newState = ServiceStates.STOPPED.toServiceState()
                    },
                    true
                )
                logger.info("Service ${service.task.name}-${service.orderedId} stopped!")

                if (!service.task.staticServices) {
                    path().toFile().deleteRecursively()
                }

                if (this.processTracking != null) {
                    processTracking!!.interrupt()
                    this.processTracking = null
                }
            }
        }

        processTracking!!.start()

        NodeCoroutineScope.launch {
            MongoUtils.updateService(service.copy(state = ServiceStates.STARTING))
            EventsService.publish(
                serviceStateChangedEvent {
                    this.service = this@LocalService.service.toDefinition()
                    this.oldState = ServiceState.SERVICE_STATE_PREPARED
                    this.newState = ServiceState.SERVICE_STATE_STARTING
                },
                true,
            )
        }
    }

    override fun stop() {
        runBlocking {
            command("stop")

            delay(5.seconds)

            if (process != null) {
                process!!.destroyForcibly()
            }
        }
    }

    override fun delete() {
        command("stop")
        NodeCoroutineScope.launch {
            delay(5.seconds)

            if (process != null) {
                process!!.destroyForcibly()
            }

            if (!service.task.staticServices) {
                synchronized(this) {
                    try {
                        Thread.sleep(200)
                    } catch (_: InterruptedException) {}
                    try {
                        if (path().exists()) {
                            path().toFile().deleteRecursively()
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to delete directory: ${path()}", e)
                    }
                }
            }
        }
    }

    override fun command(command: String) {
        try {
            if (process == null || command.isEmpty()) {
                return
            }
            val writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (_: Exception) {
            logger.error(
                "Failed to send command to service ${service.task.name}-${service.orderedId}!"
            )
        }
    }

    override fun restart() {
        NodeCoroutineScope.launch {
            command("stop")

            delay(5.seconds)

            if (process != null) {
                process!!.destroyForcibly()
            }

            start()
        }
    }
}
