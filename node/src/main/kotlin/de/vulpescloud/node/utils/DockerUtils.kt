package de.vulpescloud.node.utils

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
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
        val execCreate = dockerClient.execCreateCmd(containerId)
            .withUser("1000")
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd("mc-send-to-console", *command.split(" ").toTypedArray())
            .exec()

        dockerClient.execStartCmd(execCreate.id)
            .exec(object : ResultCallback.Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    println(String(frame.payload))
                }
            })
            .awaitCompletion()
    }
}
