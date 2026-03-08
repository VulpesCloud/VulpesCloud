package de.vulpescloud.node.services.impl.local

import de.vulpescloud.api.events.EventSerializer
import de.vulpescloud.api.events.services.ServiceLogEvent
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.services.AbstractService
import de.vulpescloud.node.utils.MongoUtils
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.exists
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class LocalService(override val service: Service) : AbstractService {

    fun path(): Path {
        return if (service.task.staticServices) {
            Path.of("local/services/${service.task.name}-${service.orderedId}")
        } else {
            Path.of("temp/services/local/${service.task.name}-${service.orderedId}")
        }
    }

    var processBuilder: ProcessBuilder? = null

    private val logger =
        LoggerFactory.getLogger("LocalService-${service.task.name}-${service.orderedId}")

    private var process: Process? = null
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
                                EventSerializer.encode(ServiceLogEvent(service, line.trim()))
                                // true,
                                /**
                                 * TODO I have temporarily disabled broadcast. In my Testing this
                                 * caused really really big issues. Especially with many
                                 * Services/heavy logging
                                 * - TheCGuy
                                 */
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
        }
    }

    override fun stop() {
        runBlocking {
            command("stop")

            delay(5000)

            if (process != null) {
                process!!.destroyForcibly()
            }
        }
    }

    override fun delete() {
        command("stop")
        NodeCoroutineScope.launch {
            delay(5000)

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
            // TODO/NOTE: For some stupid reason this doesn't seem to work with Purpur servers, i
            // don't know if its just a Purpur server not working or all Bukkit Servers
        }
    }

    override fun restart() {
        NodeCoroutineScope.launch {
            command("stop")

            delay(5000)

            if (process != null) {
                process!!.destroyForcibly()
            }

            start()
        }
    }
}
