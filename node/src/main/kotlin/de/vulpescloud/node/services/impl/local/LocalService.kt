package de.vulpescloud.node.services.impl.local

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.services.AbstractService
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.exists

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
//        if (service.state != ServiceStates.PREPARED) {
//            return
//        }

        logger.info("Service ProcessBuilder: $processBuilder")
        process = processBuilder?.start()

        Thread {
                process?.inputStream?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        println("${service.task.name}-${service.orderedId} >" + line)
                        //                    eventManagerImpl.callGlobal(
                        //                        ServiceLogEvent(
                        //                            getServiceInfo(),
                        //                            line.trim(),
                        //                        ),
                        //
                        // RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceLogEvent,
                        //                    )
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

                if (this.processTracking != null) {
                    processTracking!!.interrupt()
                    this.processTracking = null
                }
            }
        }

        processTracking!!.start()
    }

    override fun stop() {
        command("stop")
    }

    override fun delete() {
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

    override fun command(command: String) {
        try {
            if (process == null || command.isEmpty()) {
                return
            }
            val writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (_: IOException) {}
    }

    override fun restart() {
        stop()
        start()
    }
}
