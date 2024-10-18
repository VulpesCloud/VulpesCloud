package io.github.thecguygithub.node.service.utils

import io.github.thecguygithub.api.platforms.PlatformTypes
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.node.Node
import java.net.InetSocketAddress
import java.net.ServerSocket


object ServicePortDetector {
    fun detectServicePort(task: ClusterTask): Int {
        val platformType = task.platform()?.type
        var serverPort: Int = task.startPort()


        while (isUsed(serverPort)) {
            serverPort++
        }

        return serverPort
    }

    private fun isUsed(port: Int): Boolean {
        for (service in Node.serviceProvider?.services()!!) {
            if (service.port() == port) {
                return true
            }
        }
        try {
            ServerSocket().use { testSocket ->
                testSocket.bind(InetSocketAddress(port))
                return false
            }
        } catch (e: Exception) {
            return true
        }
    }
}