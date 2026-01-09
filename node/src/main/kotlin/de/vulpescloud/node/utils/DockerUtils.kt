package de.vulpescloud.node.utils

import com.github.dockerjava.core.DockerClientImpl
import de.vulpescloud.node.Node

object DockerUtils {
    private val dockerClient = DockerClientImpl.getInstance(Node.instance.dockerClientConfig, Node.instance.dockerHttpClient)

    fun getContainerIdByName(name: String): String? {
        val containers = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withNameFilter(listOf(name))
            .exec()
        return if (containers.isNotEmpty()) containers[0].id else null
    }

    fun execMinecraftCommand(containerId: String, command: String) {

    }
}
