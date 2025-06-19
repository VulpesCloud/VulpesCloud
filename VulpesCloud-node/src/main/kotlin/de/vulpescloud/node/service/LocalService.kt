package de.vulpescloud.node.service

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.service.ServiceLogEvent
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.player.Player
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.AbstractService
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.api.task.Task
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.event.EventManagerImpl
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.exists

class LocalService(
    override val task: Task,
    override val uuid: UUID,
    override val orderedId: Int,
    override val port: Int,
    override val runningNode: ClusterNode,
    override var state: ServiceStates,
    override val maxPlayers: Int,
    override val onlinePlayerCount: Int,
    override val name: String = "${task.name}-$orderedId",
    override val onlinePlayers: List<Player>,
    override val environmentVars: List<Pair<String, String>>,

    private val serviceProvider: ServiceProvider,
    private val eventManager: EventManager,
) : AbstractService() {
    private val eventManagerImpl = eventManager as EventManagerImpl
    private val logger = LoggerFactory.getLogger("LocalService-$name")

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

    override fun start() {
        if (state != ServiceStates.PREPARED) {
            return
        }

        state = ServiceStates.STARTING
        getRC()?.setHashField("VULPESCLOUD:SERVICES", name, JSONObject(getServiceInfo()).toString())
        eventManagerImpl.callGlobal(
            ServiceStateChangeEvent(
                getServiceInfo(),
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
                            getServiceInfo(),
                            line.trim(),
                        ),
                        RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceLogEvent,
                    )
                }
            }
        }.start()

        this.processTracking = Thread {
            try {
                synchronized(this) { process?.waitFor() }
            } catch (_: InterruptedException) {}
            if (state != ServiceStates.STOPPING) {
                if (process != null) {
                    process!!.exitValue()
                }
                this.postShutdownProcess()
            }
        }

        processTracking!!.start()
    }

    override fun sendCommand(command: String) {
        try {
            if (process == null || command.isEmpty()) {
                return
            }
            val writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (e: IOException) {
            if (serviceProvider.getServiceByUUID(this.uuid)?.state != ServiceStates.STOPPING) {
                logger.error("Failed to send command to process: $name", e)
            }
        }
    }

    override fun forceStop() {
        process?.toHandle()?.destroyForcibly()
        postShutdownProcess()
    }

    override fun stop() {
        sendCommand("stop")
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

        logger.info("The service &8'&f${name}&8' &7is stopped now&8!")
        (serviceProvider as ServiceProviderImpl).localServices.remove(this)
        getRC()?.deleteHashField("VULPESCLOUD:SERVICES", name)
    }

}