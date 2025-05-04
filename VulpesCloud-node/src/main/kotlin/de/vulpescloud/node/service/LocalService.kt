package de.vulpescloud.node.service

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.service.ServiceLogEvent
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.api.task.Task
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.event.EventManagerImpl
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import org.slf4j.LoggerFactory

data class LocalService(
    val task: Task,
    val uuid: UUID,
    val orderedId: Int,
    val port: Int,
    val runningNode: ClusterNode,
    var state: ServiceStates,
    val maxPlayers: Int,
    val onlinePlayerCount: Int,
    val name: String = "${task.name}-$orderedId",
    val onlinePlayers: List<Player>,
    val environmentVars: List<Pair<String, String>>,
    private val serviceProvider: ServiceProvider,
    private val eventManager: EventManager,
) {
    private val serviceProviderImpl = serviceProvider as ServiceProviderImpl
    private val logger = LoggerFactory.getLogger(LocalService::class.java)
    private val eventManagerImpl = eventManager as EventManagerImpl

    fun path(): Path {
        return if (task.staticServices) {
            Path.of("local/services/${task.name}/$name")
        } else {
            Path.of("temp/services/${task.name}/$name")
        }
    }

    var processBuilder: ProcessBuilder? = null

    private var process: Process? = null
    private var processTracking: Thread? = null

    fun start() {
        val service = Service(
            task,
            uuid,
            orderedId,
            port,
            runningNode,
            ServiceStates.STARTING,
            maxPlayers,
            onlinePlayerCount,
            name,
            onlinePlayers,
            environmentVars,
        )
        getRC()?.setHashField("VULPESCLOUD_SERVICES", name, JSONObject(service).toString())
        eventManagerImpl.callGlobal(
            ServiceStateChangeEvent(
                service,
                ServiceStates.PREPARED,
                ServiceStates.STARTING
            ),
            RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceStateChangeEvent
        )

        process = processBuilder?.start()

        Thread {
                process?.inputStream?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        eventManagerImpl.callGlobal(
                            ServiceLogEvent(
                                serviceProvider.getServiceByUUID(this.uuid)!!,
                                line.trim(),
                            ),
                            RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceLogEvent,
                        )
                        logger.debug("&8[ &m{} &8] &b{}", name, line.trim())
                    }
                }
            }
            .start()

        this.processTracking = Thread {
            try {
                synchronized(this) { process?.waitFor() }
            } catch (e: InterruptedException) {
                logger.debug("Exception: {}", e.printStackTrace())
            }
            if (state != ServiceStates.STOPPING) {
                if (process != null) {
                    process!!.exitValue()
                }
                this.postShutdownProcess()
            }
        }

        processTracking!!.start()
    }

    fun sendCommand(command: String) {
        try {
            if (process == null || command.isEmpty()) {
                return
            }
            val writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (e: Exception) {
            logger.error("Sending command to process failed: ", e)
        }
    }

    fun forceStop() {
        process?.toHandle()?.destroyForcibly()
        postShutdownProcess()
    }

    private fun postShutdownProcess() {
        this.process = null

        if (this.processTracking != null) {
            processTracking!!.interrupt()
            this.processTracking = null
        }

        if (!task.staticServices) {
            synchronized(this) {
                try {
                    Thread.sleep(200)
                } catch (ignore: InterruptedException) {}
                try {
                    if (path().exists()) {
                        Files.walk(path()).sorted(Comparator.reverseOrder()).forEach {
                            Files.delete(it)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete directory: ${path()}", e)
                }
            }
        }

        logger.info("The service &8'&f${name}&8' &7is stopped now&8!")
        val localServices = serviceProviderImpl.localServices
        localServices.remove(this)
        getRC()?.deleteHashField("VULPESCLOUD_SERVICES", name)
    }
}
