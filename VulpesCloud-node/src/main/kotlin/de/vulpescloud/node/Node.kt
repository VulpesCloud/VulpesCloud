package de.vulpescloud.node

import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.language.LanguageProvider
import de.vulpescloud.node.cluster.ClusterProvider
import de.vulpescloud.node.command.provider.CommandProvider
import de.vulpescloud.node.commands.*
import de.vulpescloud.node.config.LogLevels
import de.vulpescloud.node.event.events.NodeStateChangeEvent
import de.vulpescloud.node.event.events.TaskUpdateEvent
import de.vulpescloud.node.networking.mysql.MySQLController
import de.vulpescloud.node.networking.redis.RedisConnectionChecker
import de.vulpescloud.node.networking.redis.RedisController
import de.vulpescloud.node.service.ServiceProvider
import de.vulpescloud.node.service.ServiceStartScheduler
import de.vulpescloud.node.task.TaskProvider
import de.vulpescloud.node.template.TemplateProvider
import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.util.Configurations.readContent
import de.vulpescloud.node.version.VersionProvider
import org.slf4j.LoggerFactory
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

        lateinit var logger: org.slf4j.Logger
            private set

        lateinit var languageProvider: LanguageProvider
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

        languageProvider = LanguageProvider()

        languageProvider.setLang(nodeConfig!!.language)

        languageProvider.loadLangFilesFromClassPath()

        logger = LoggerFactory.getLogger(Node::class.java)

        redisController = RedisController()

        mySQLController = MySQLController()

        mySQLController.createDefaultTables()

        NodeStateChangeEvent
        TaskUpdateEvent

        clusterProvider = ClusterProvider()

        versionProvider = VersionProvider()

        taskProvider = TaskProvider()

        templateProvider = TemplateProvider()

        serviceProvider = ServiceProvider()

        serviceProvider.getAllServiceFromRedis()

        commandProvider = CommandProvider()

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

        ServiceStartScheduler().schedule()

        logger.info(
            languageProvider.translate("node.boot.success.message"),
            System.currentTimeMillis() - System.getProperty("startup").toLong()
        )

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