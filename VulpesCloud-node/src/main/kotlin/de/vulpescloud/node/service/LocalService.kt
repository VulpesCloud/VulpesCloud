package de.vulpescloud.node.service

import de.vulpescloud.api.network.redis.RedisHashNames
import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ClusterServiceStates
import de.vulpescloud.api.services.builder.ServiceEventMessageBuilder
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.util.DirectoryActions
import de.vulpescloud.node.version.Version
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Suppress("EmptyMethod")
class LocalService(
    task: Task,
    orderedId: Int,
    id: UUID,
    port: Int,
    hostname: String?,
    runningNode: String?
) :
    Service(task, orderedId, id, port, hostname!!, runningNode!!) {

    private val logger = LoggerFactory.getLogger(LocalService::class.java)

    private var process: Process? = null
    private var processTracking: Thread? = null

    val runningDir: Path

    init {
        // set the default max players value to the parent value
        maxPlayers = task.maxPlayers()

        this.runningDir =
            if (task.staticService()) Path.of(("static/" + task.name()) + "-" + orderedId) else Path.of("running/" + name() + "-" + id)
        runningDir.toFile().mkdirs()
    }

    fun updateLocalServiceState(status: ClusterServiceStates) {
        state = status
        Node.instance!!.getRC()?.sendMessage(
            ServiceEventMessageBuilder.stateEventBuilder()
                .setService(this)
                .setState(status)
                .build(),
            RedisPubSubChannels.VULPESCLOUD_EVENT_SERVICE.name
        )
        Node.instance!!.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, name(), JSONObject(this).toString())
    }

    override fun shutdown() {
        Node.serviceProvider.factory().shutdownService(this)
    }

    fun start(builder: ProcessBuilder) {
        this.process = builder.start()

        Thread {
            process?.inputStream?.bufferedReader()?.use { reader ->
                reader.forEachLine { line ->
                    // todo Make better service console logging
                    logger.debug("[${name()}] $line")
                }
            }
        }.start()

        this.processTracking = Thread {
            // if player send a stop command from game command system
            try {
                synchronized(this) {
                    process?.waitFor()
                }
            } catch (e: InterruptedException) {
                logger.debug("Exception: {}", e.printStackTrace())
            }
            if (state() != ClusterServiceStates.STOPPING) {
                // Node.instance?.getRC()?.sendMessage("SERVICE;${this.name()};EVENT;STOP", "testcloud-service-events")
                if (process != null) {
                    process!!.exitValue()
                }
                this.postShutdownProcess()
            }
        }

        processTracking!!.start()
    }

    override fun executeCommand(command: String) {
        if (!hasProcess() || command.isEmpty()) {
            return
        }
        val writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
        writer.write(command)
        writer.newLine()
        writer.flush()
    }

    fun destroyService() {
        if (process != null) {
            state()
            process!!.toHandle().destroyForcibly()
            this.postShutdownProcess()
        }
    }

    private fun postShutdownProcess() {
        this.process = null

        if (this.processTracking != null) {
            processTracking!!.interrupt()
            this.processTracking = null
        }

        if (!task.staticService()) {
            synchronized(this) {
                try {
                    Thread.sleep(200)
                } catch (ignore: InterruptedException) {
                }
                try {
                    if (DirectoryActions.delete(runningDir)) {
                        Files.deleteIfExists(runningDir)
                    } else {
                        logger.info("Cannot shutdown ${name()} cleanly! Files are already deleted")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        logger.info("The service &8'&f${name()}&8' &7is stopped now&8!")

        // todo Update the Service in the cluster
        Node.instance!!.getRC()?.deleteHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, name())
    }

    private fun hasProcess(): Boolean {
        return this.process != null
    }

    fun version(): Version? {
        return Node.versionProvider.search(task().version().name)
    }
}