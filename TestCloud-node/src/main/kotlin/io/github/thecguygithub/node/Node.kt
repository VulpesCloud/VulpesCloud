package io.github.thecguygithub.node

import io.github.thecguygithub.api.JavaCloudAPI
import io.github.thecguygithub.api.services.ClusterServiceProvider
import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import io.github.thecguygithub.node.command.CommandProvider
import io.github.thecguygithub.node.event.NodeEventListener
import io.github.thecguygithub.node.networking.RedisController
import io.github.thecguygithub.node.terminal.JLineTerminal
import io.github.thecguygithub.node.util.Configurations.readContent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path


class Node: JavaCloudAPI() {

    private val logger: Logger = LoggerFactory.getLogger(Node::class.java)



    companion object {
        var instance: Node? = null
            private set

        var nodeConfig: NodeConfig? = null
            private set

        var terminal: JLineTerminal? = null
            private set

        var commandProvider: CommandProvider? = null
            private set

        var redisController: RedisController? = null
            private set
    }

    init {
        instance = this


        nodeConfig = readContent(Path.of("config.json"), NodeConfig())

        terminal = JLineTerminal(nodeConfig!!)

        redisController = RedisController()

        NodeEventListener

        redisController?.sendMessage("EVENT;NODE;${nodeConfig?.localNode};STATUS;STARTING", "testcloud-events-nodes-status")

        commandProvider = CommandProvider()

        terminal!!.allowInput()

        redisController?.sendMessage("EVENT;NODE;${nodeConfig?.localNode};STATUS;RUNNING", "testcloud-events-nodes-status")

    }

    fun getRC(): RedisController? {
        return redisController
    }

    override fun serviceProvider(): ClusterServiceProvider {
        return getInstance().serviceProvider()
    }

    override fun taskProvider(): ClusterTaskProvider {
        return getInstance().taskProvider()
    }
}