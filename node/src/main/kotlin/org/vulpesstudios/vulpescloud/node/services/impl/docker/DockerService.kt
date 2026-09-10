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

package org.vulpesstudios.vulpescloud.node.services.impl.docker

import com.github.dockerjava.core.DockerClientImpl
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.services.Service
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.services.AbstractService
import org.vulpesstudios.vulpescloud.node.utils.DockerUtils
import java.nio.file.Path

class DockerService(override var service: Service) : AbstractService {
    private val dockerClient = DockerClientImpl.getInstance(Node.instance.dockerClientConfig, Node.instance.dockerHttpClient)
    private val logger = LoggerFactory.getLogger("DockerService-${service.task.name}-${service.orderedId}")

    override fun start() {
        val containerId = DockerUtils.getContainerIdByName(getContainerName())
        if (containerId != null) {
            dockerClient.startContainerCmd(containerId).exec()
        } else {
            logger.warn("Container for service ${service.task.name}-${service.orderedId} not found. Could not start stopped/prepared service.")
        }
    }

    override fun stop() {
        val containerId = DockerUtils.getContainerIdByName(getContainerName())
        if (containerId != null) {
            dockerClient.stopContainerCmd(containerId).exec()
        } else {
            logger.warn("Container for service ${service.task.name}-${service.orderedId} not found. Skipping stop and remove.")
        }
    }

    override fun delete() {
        if (service.task.staticServices) {
            val containerId = DockerUtils.getContainerIdByName(getContainerName())
            if (containerId != null) {
                dockerClient.stopContainerCmd(containerId).exec()
                dockerClient.removeContainerCmd(containerId).exec()
            } else {
                logger.warn("Container for service ${service.task.name}-${service.orderedId} not found. Skipping stop and remove.")
            }
        } else {
            val containerId = DockerUtils.getContainerIdByName(getContainerName())
            if (containerId != null) {
                dockerClient.stopContainerCmd(containerId).exec()
                dockerClient.removeContainerCmd(containerId).exec()
            } else {
                logger.warn("Container for service ${service.task.name}-${service.orderedId} not found. Skipping stop and remove.")
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
            logger.warn("Container for service ${service.task.name}-${service.orderedId} not found. Could not execute command.")
        }
    }

    override fun restart() {
        val containerId = DockerUtils.getContainerIdByName(getContainerName())
        if (containerId != null) {
            dockerClient.restartContainerCmd(containerId).exec()
        } else {
            logger.warn("Container for service ${service.task.name}-${service.orderedId} not found. Could not restart running service.")
        }
    }

    fun getContainerName(): String {
        return "${service.task.name}-${service.orderedId}-${service.uuid}"
    }

    override fun path(): Path {
        return if (service.task.staticServices) {
            Path.of("local/services/${service.task.name}-${service.orderedId}")
        } else {
            Path.of("temp/services/docker/${service.task.name}-${service.orderedId}")
        }
    }
}