package io.github.thecguygithub.node

import io.github.thecguygithub.api.cluster.NodeStates
import io.github.thecguygithub.node.cluster.ClusterProvider
import io.github.thecguygithub.node.command.provider.CommandProvider
import io.github.thecguygithub.node.commands.*
import io.github.thecguygithub.node.config.LogLevels
import io.github.thecguygithub.node.event.events.NodeStateChangeEvent
import io.github.thecguygithub.node.event.events.TaskUpdateEvent
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.mysql.MySQLController
import io.github.thecguygithub.node.networking.redis.RedisConnectionChecker
import io.github.thecguygithub.node.networking.redis.RedisController
import io.github.thecguygithub.node.service.ServiceProvider
import io.github.thecguygithub.node.task.TaskProvider
import io.github.thecguygithub.node.template.TemplateProvider
import io.github.thecguygithub.node.terminal.JLineTerminal
import io.github.thecguygithub.node.util.Configurations.readContent
import io.github.thecguygithub.node.version.VersionProvider
import java.nio.file.Path
import kotlin.system.exitProcess


class Node {

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

        lateinit var mySQLController: MySQLController
            private set

        lateinit var versionProvider: VersionProvider
            private set

        lateinit var taskProvider: TaskProvider
            private set

        lateinit var clusterProvider: ClusterProvider
            private set

        lateinit var serviceProvider: ServiceProvider
            private set

        lateinit var templateProvider: TemplateProvider
            private set

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

        logger.debug("Initializing MySQL Controller")

        mySQLController = MySQLController()

        logger.debug("Creating MySQL Tables")

        mySQLController.createDefaultTables()

        logger.debug("Loading Events!")

        NodeStateChangeEvent
        TaskUpdateEvent

        logger.debug("Initializing ClusterProvider")

        clusterProvider = ClusterProvider()

        logger.debug("Initializing VersionProvider")

        versionProvider = VersionProvider()

        logger.debug("Initializing TaskProvider")

        taskProvider = TaskProvider()

        logger.debug("Initializing TemplateProvider")

        templateProvider = TemplateProvider()

        logger.debug("Initializing ServiceProvider")

        serviceProvider = ServiceProvider()

        logger.debug("Initializing CommandProvider")

        commandProvider = CommandProvider()

        logger.debug("Registering Commands!")

        ClearCommand()
        HelpCommand()
        InfoCommand()
        ShutdownCommand()
        VersionCommand()
        TasksCommand()
        ClusterCommand()
        ServiceCommand()
        DevCommand()

        Runtime.getRuntime().addShutdownHook(Thread())

        RedisConnectionChecker().schedule()

        terminal!!.allowInput()

        clusterProvider.updateLocalNodeState(NodeStates.ONLINE)
    }

//    fun updateConfig() {
//        writeContent(Path.of("config.json"), nodeConfig)
//    }

    fun getRC(): RedisController? {
        return redisController
    }
}