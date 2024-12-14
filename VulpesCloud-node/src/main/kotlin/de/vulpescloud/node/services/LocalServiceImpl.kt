package de.vulpescloud.node.services

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.version.Version
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class LocalServiceImpl(
    override val task: Task,
    override val orderedId: Int,
    override val id: UUID,
    override val port: Int,
    override val hostname: String,
    override val runningNode: String,
    override var state: ServiceStates = ServiceStates.LOADING,
    override var logging: Boolean = false
) : Service(task, orderedId, id, port, hostname, runningNode, state = state, logging = logging) {

    private val logger = LoggerFactory.getLogger(LocalServiceImpl::class.java)
    private var process: Process? = null
    private var processTracking: Thread? = null
    val runningDir: Path = if (task.staticService()) {
        Path.of("local/services/${task.name()}-${orderedId()}")
    } else {
        Path.of("local/temp/services/${task.name()}-${orderedId()}")
    }

    init {

        this.runningDir.toFile().mkdirs()
    }

    fun start(builder: ProcessBuilder) {
        this.process = builder.start()

        updateState(ServiceStates.CONNECTING)

        Thread {
            process?.inputStream?.bufferedReader()?.use { reader ->
                reader.forEachLine { line ->
                    if (Node.instance.serviceProvider.isLogging(this)) {
                        logger.info("&8[ &m{} &8] &b{}", name(), line.trim())
                    }
//                    val msg = ServiceEventMessageBuilder.consoleEventBuilder()
//                        .setService(this)
//                        .setLine(line.trim())
//                        .build()
//                    Node.instance!!.getRC()?.sendMessage(msg, RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name)
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
            if (state() != ServiceStates.STOPPING) {
                // Node.instance?.getRC()?.sendMessage("SERVICE;${this.name()};EVENT;STOP", "testcloud-service-events")
                if (process != null) {
                    process!!.exitValue()
                }
                this.postShutdownProcess()
            }
        }

        processTracking!!.start()
    }

    fun executeCommand(command: String) {
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
                    Files.deleteIfExists(runningDir)
//                    if (DirectoryActions.delete(runningDir)) {
//                        Files.deleteIfExists(runningDir)
//                    } else {
//                        logger.info("Cannot shutdown ${name()} cleanly! Files are already deleted")
//                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        logger.info("The service &8'&f${name()}&8' &7is stopped now&8!")

        Node.instance.getRC()?.deleteHashField(RedisHashNames.VULPESCLOUD_SERVICES.name, name())
    }

    private fun hasProcess(): Boolean {
        return this.process != null
    }

    fun version(): Version? {
        return Node.instance.versionProvider.getByName(task.version().environment)
    }

}