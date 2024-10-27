package io.github.thecguygithub.node.service

import io.github.thecguygithub.api.services.ClusterServiceStates
import io.github.thecguygithub.api.tasks.Task
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.util.DirectoryActions
import io.github.thecguygithub.node.version.Version
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class LocalService(
    task: Task,
    orderedId: Int,
    id: UUID,
    port: Int,
    hostname: String?,
    runningNode: String?
) :
    Service(task, orderedId, id, port, hostname!!, runningNode!!) {

    val logger = Logger()

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

    override fun shutdown() {
        Node.serviceProvider.factory().shutdownService(this)
    }

    fun start(builder: ProcessBuilder) {
        this.process = builder.start()

        this.processTracking = Thread {
            // if player send a stop command from game command system
            try {
                synchronized(this) {
                    process?.waitFor()
                }
            } catch (ignore: InterruptedException) {
            }
            if (state(ClusterServiceStates.STARTING) != ClusterServiceStates.STOPPING) {
                Node.instance?.getRC()?.sendMessage("SERVICE;${this.name()};EVENT;STOP", "testcloud-service-events")
                state(ClusterServiceStates.STOPPING)
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
            state(ClusterServiceStates.STOPPING)
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
                // todo check for windows directory not empty exception
                try {
                    Thread.sleep(200)
                } catch (ignore: InterruptedException) {
                }
                try {
                    if (DirectoryActions.delete(runningDir)) {
                        Files.deleteIfExists(runningDir)
                    } else {
                        logger.info("Cannot shutdown ${name()} cleanly! Files are already exists")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        logger.info("The service &8'&f${name()}&8' &7is stopped now&8!")

        // alert the event before remove it from the local cache
        Node.instance?.getRC()?.sendMessage("SERVICE;${this.name()};EVENT;STOPPED", "testcloud-service-events")

        // Node.instance().clusterProvider().broadcastAll(ClusterSyncUnregisterServicePacket(id()))
    }

    private fun hasProcess(): Boolean {
        return this.process != null
    }

    fun version(): Version? {
        return Node.versionProvider.search(task().version().name)
    }
}