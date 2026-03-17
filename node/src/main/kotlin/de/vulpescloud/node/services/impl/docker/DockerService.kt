package de.vulpescloud.node.services.impl.docker

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import com.github.dockerjava.core.DockerClientImpl
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.services.AbstractService
import de.vulpescloud.node.utils.DockerUtils
import java.io.Closeable
import java.nio.file.Path
import org.slf4j.LoggerFactory

class DockerService(override val service: Service) : AbstractService {
    private val dockerClient =
        DockerClientImpl.getInstance(
            Node.instance.dockerClientConfig,
            Node.instance.dockerHttpClient,
        )
    private val logger =
        LoggerFactory.getLogger("DockerService-${service.task.name}-${service.orderedId}")

    private var logStream: Closeable? = null

    override fun start() {
        val containerId = DockerUtils.getContainerIdByName(getContainerName())
        if (containerId != null) {
            dockerClient.startContainerCmd(containerId).exec()
            streamLogs(containerId)
        } else {
            logger.warn(
                "Container for service ${service.task.name}-${service.orderedId} not found. Could not start stopped/prepared service."
            )
        }
    }

    override fun stop() {
        logStream?.close()
        logStream = null
        val containerId = DockerUtils.getContainerIdByName(getContainerName())
        if (containerId != null) {
            dockerClient.stopContainerCmd(containerId).exec()
        } else {
            logger.warn(
                "Container for service ${service.task.name}-${service.orderedId} not found. Skipping stop and remove."
            )
        }
    }

    override fun delete() {
        logStream?.close()
        logStream = null
        if (service.task.staticServices) {
            val containerId = DockerUtils.getContainerIdByName(getContainerName())
            if (containerId != null) {
                dockerClient.stopContainerCmd(containerId).exec()
                dockerClient.removeContainerCmd(containerId).exec()
            } else {
                logger.warn(
                    "Container for service ${service.task.name}-${service.orderedId} not found. Skipping stop and remove."
                )
            }
        } else {
            val containerId = DockerUtils.getContainerIdByName(getContainerName())
            if (containerId != null) {
                dockerClient.stopContainerCmd(containerId).exec()
                dockerClient.removeContainerCmd(containerId).exec()
            } else {
                logger.warn(
                    "Container for service ${service.task.name}-${service.orderedId} not found. Skipping stop and remove."
                )
            }
            val servicePath = path()
            if (servicePath.toFile().exists()) {
                servicePath.toFile().deleteRecursively()
            }
        }
    }

    override fun command(command: String) {
        val containerId = DockerUtils.getContainerIdByName(getContainerName())
        if (containerId != null) {
            DockerUtils.execMinecraftCommand(containerId, command)
        } else {
            logger.warn(
                "Container for service ${service.task.name}-${service.orderedId} not found. Could not execute command."
            )
        }
    }

    override fun restart() {
        logStream?.close()
        logStream = null
        val containerId = DockerUtils.getContainerIdByName(getContainerName())
        if (containerId != null) {
            dockerClient.restartContainerCmd(containerId).exec()
            streamLogs(containerId)
        } else {
            logger.warn(
                "Container for service ${service.task.name}-${service.orderedId} not found. Could not restart running service."
            )
        }
    }

    fun getContainerName(): String {
        return "${service.task.name}-${service.orderedId}-${service.uuid}"
    }

    fun path(): Path {
        return if (service.task.staticServices) {
            Path.of("local/services/${service.task.name}-${service.orderedId}")
        } else {
            Path.of("temp/services/docker/${service.task.name}-${service.orderedId}")
        }
    }

    private fun streamLogs(containerId: String) {
        logStream?.close()
        logStream = dockerClient
            .logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .withTailAll()
            .exec(object : ResultCallback.Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    val line = String(frame.payload).trimEnd()
                    if (line.isBlank()) return
                    when (frame.streamType) {
                        StreamType.STDERR -> logger.error("[container] $line")
                        else              -> logger.info("[container] $line")
                    }
                }

                override fun onError(throwable: Throwable) {
                    logger.warn(
                        "Log stream error for ${service.task.name}-${service.orderedId}: ${throwable.message}"
                    )
                }
            })
    }
}