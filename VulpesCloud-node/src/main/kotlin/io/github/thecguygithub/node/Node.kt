package io.github.thecguygithub.node

import io.github.thecguygithub.node.command.provider.CommandProvider
import io.github.thecguygithub.node.commands.*
import io.github.thecguygithub.node.config.LogLevels
import io.github.thecguygithub.node.event.NodeEventListener
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.mysql.MySQLController
import io.github.thecguygithub.node.networking.redis.RedisController
import io.github.thecguygithub.node.terminal.JLineTerminal
import io.github.thecguygithub.node.util.Configurations.readContent
import io.github.thecguygithub.node.version.VersionProvider
import java.nio.file.Path
import kotlin.system.exitProcess


class Node {

    companion object {

        // var nodeStatus: NodeSituation = NodeSituation.INITIALIZING

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

        var mySQLController: MySQLController? = null
            private set

        lateinit var versionProvider: VersionProvider

        lateinit var logger: Logger
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

        logger = Logger()

        logger.debug("Terminal initialized! Continuing Startup!")

        logger.debug("Loading Redis Controller")

        redisController = RedisController()

        logger.debug("Loading Events!")

        NodeEventListener

        redisController?.sendMessage(
            "EVENT;NODE;${nodeConfig?.localNode};STATUS;&eSTARTING",
            "testcloud-events-nodes-status"
        )

        logger.debug("Initializing MySQL Controller")

        mySQLController = MySQLController()

        logger.debug("Initializing VersionProvider")

        versionProvider = VersionProvider()


        logger.debug("Initializing CommandProvider")

        commandProvider = CommandProvider()

        logger.debug("Registering Commands!")

        ClearCommand()
        HelpCommand()
        InfoCommand()
        ShutdownCommand()
        VersionCommand()
//        TasksCommand()


        Runtime.getRuntime().addShutdownHook(Thread())

        terminal!!.allowInput()

        redisController?.sendMessage(
            "EVENT;NODE;${nodeConfig?.localNode};STATUS;&2RUNNING",
            "testcloud-events-nodes-status"
        )

    }

    fun getRC(): RedisController? {
        return redisController
    }
}