package de.vulpescloud.node

import de.vulpescloud.api.cluster.NodeStates
import de.vulpescloud.api.language.LanguageProvider
import de.vulpescloud.node.cluster.ClusterProvider
import de.vulpescloud.node.command.provider.CommandProvider
import de.vulpescloud.node.commands.*
import de.vulpescloud.node.config.LogLevels
import de.vulpescloud.node.event.events.NodeStateChangeEvent
import de.vulpescloud.node.event.events.TaskUpdateEvent
import de.vulpescloud.node.event.events.task.TaskCreateEvent
import de.vulpescloud.node.module.ModuleProvider
import de.vulpescloud.node.networking.mysql.MySQLController
import de.vulpescloud.node.networking.redis.RedisConnectionChecker
import de.vulpescloud.node.networking.redis.RedisController
import de.vulpescloud.node.player.PlayerProvider
import de.vulpescloud.node.service.ServiceProvider
import de.vulpescloud.node.service.ServiceStartScheduler
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setups.NodeSetup
import de.vulpescloud.node.task.TaskProvider
import de.vulpescloud.node.template.TemplateProvider
import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.util.Configurations.readContent
import de.vulpescloud.node.version.VersionProvider
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

class Node {

    val setupLock = ReentrantLock()
    val setupCondition: Condition = setupLock.newCondition()

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

        lateinit var setupProvider: SetupProvider
            private set

        lateinit var playerProvider: PlayerProvider
            private set

        lateinit var moduleProvider: ModuleProvider
            private set
    }

    init {
        instance = this

        nodeConfig = readContent(Path.of("config.json"), NodeConfig())

        setupProvider = SetupProvider()

        terminal = JLineTerminal(nodeConfig!!)

        if (!LogLevels.entries.contains(nodeConfig?.logLevel)) {
            terminal!!.printLine("INVALID CONFIGURATION HAS BEEN FOUND! PLEASE CHECK YOUR CONFIG!")
            exitProcess(1)
        }

        logger = LoggerFactory.getLogger(Node::class.java)

        languageProvider = LanguageProvider()

        languageProvider.setLang(nodeConfig!!.language)

        languageProvider.loadLangFilesFromClassPath()

        if (!nodeConfig!!.ranFirstSetup) {
            CompletableFuture.runAsync {
                setupLock.withLock {
                    terminal!!.allowInput()
                    setupProvider.startSetup(NodeSetup())
                }
            }
        }

        setupLock.withLock {
            while (!nodeConfig!!.ranFirstSetup) {
                setupCondition.await()
            }
        }

        redisController = RedisController()

        mySQLController = MySQLController()

        clusterProvider = ClusterProvider()

        versionProvider = VersionProvider()

        taskProvider = TaskProvider()

        templateProvider = TemplateProvider()

        serviceProvider = ServiceProvider()

        playerProvider = PlayerProvider()

        moduleProvider = ModuleProvider()

        commandProvider = CommandProvider()

        // Setting Finished, doing other stuff like initialization now

        mySQLController.createDefaultTables()

        moduleProvider.loadAllModules()

        NodeStateChangeEvent
        TaskUpdateEvent
        TaskCreateEvent

        serviceProvider.getAllServiceFromRedis()

        ClearCommand()
        HelpCommand()
        InfoCommand()
        ShutdownCommand()
        VersionCommand()
        TasksCommand()
        ClusterCommand()
        ServiceCommand()
        DevCommand()
        PlayerCommand()

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

    fun getRC(): RedisController? {
        return redisController
    }
}