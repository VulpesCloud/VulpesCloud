package io.github.thecguygithub.node

import io.github.thecguygithub.api.JavaCloudAPI
import io.github.thecguygithub.api.services.ClusterServiceProvider
import io.github.thecguygithub.api.tasks.ClusterTaskProvider
import io.github.thecguygithub.node.command.CommandProvider
import io.github.thecguygithub.node.config.LogLevels
import io.github.thecguygithub.node.event.NodeEventListener
import io.github.thecguygithub.node.networking.RedisController
import io.github.thecguygithub.node.terminal.JLineTerminal
import io.github.thecguygithub.node.util.Configurations.readContent
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.tasks.ClusterTaskProviderImpl
import java.nio.file.Path
import kotlin.system.exitProcess


class Node: JavaCloudAPI() {

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

        var taskProvider: ClusterTaskProviderImpl? = null
            private set
    }

    init {
        instance = this


        nodeConfig = readContent(Path.of("config.json"), NodeConfig())

        terminal = JLineTerminal(nodeConfig!!)

        if (!LogLevels.entries.contains(nodeConfig?.logLevel)) {
            terminal!!.printLine("INVALID CONFIGURATION HAS BEEN FOUND! PLEASE CHECK YOUR CONFIG!")
            exitProcess(1)
        }

        val logger = Logger()

        logger.debug("Terminal initialized! Continuing Startup!")

        logger.debug("Loading Redis Controller")

        redisController = RedisController()

        logger.debug("Loading Events!")

        NodeEventListener

        redisController?.sendMessage("EVENT;NODE;${nodeConfig?.localNode};STATUS;&eSTARTING", "testcloud-events-nodes-status")


        taskProvider = ClusterTaskProviderImpl()

        commandProvider = CommandProvider()

        terminal!!.allowInput()

        redisController?.sendMessage("EVENT;NODE;${nodeConfig?.localNode};STATUS;&2RUNNING", "testcloud-events-nodes-status")

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