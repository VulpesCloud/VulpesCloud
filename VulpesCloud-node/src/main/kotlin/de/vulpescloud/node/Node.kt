package de.vulpescloud.node

import de.vulpescloud.api.language.Translator
import de.vulpescloud.api.utils.StringUtils
import de.vulpescloud.node.command.provider.CommandProvider
import de.vulpescloud.node.commands.*
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.networking.mysql.MySQLController
import de.vulpescloud.node.networking.redis.RedisController
import de.vulpescloud.node.player.VulpesPlayerProvider
import de.vulpescloud.node.player.redis.PlayerEventListener
import de.vulpescloud.node.schedulers.ServiceStartScheduler
import de.vulpescloud.node.services.ServiceProvider
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setups.FirstSetup
import de.vulpescloud.node.tasks.TaskProvider
import de.vulpescloud.node.template.TemplateProvider
import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.version.VersionProvider
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Node {
    val setupLock = ReentrantLock()
    val setupCondition: Condition = setupLock.newCondition()

    val config = ConfigProvider()
    val terminal = JLineTerminal()
    val commandProvider = CommandProvider()
    val setupProvider = SetupProvider()
    private val translator = Translator
    private val logger = LoggerFactory.getLogger(Node::class.java)
    private var redisController: RedisController? = null
    private var mysqlController: MySQLController? = null
    val versionProvider = VersionProvider()
    val taskProvider = TaskProvider()
    val templateProvider = TemplateProvider()
    val serviceProvider = ServiceProvider()
    val forwardingSecret = StringUtils.generateRandomString(8)
    val playerProvider = VulpesPlayerProvider()

    init {
        instance = this
        translator.setLang(config.language)
        translator.loadLangFilesFromClassPath()
        terminal.initialize()

        if (!config.ranFirstSetup) {
            CompletableFuture.runAsync {
                setupLock.withLock {
                    terminal.allowInput()
                    setupProvider.startSetup(FirstSetup())
                }
            }
            setupLock.withLock {
                setupCondition.await()
            }
        }

        redisController = RedisController()
        mysqlController = MySQLController()

        mysqlController?.generateDefaultTables()

        versionProvider.initialize()

        taskProvider.tasks().forEach {
            logger.debug("Preparing a template for task &m${it.name()}")
            templateProvider.prepareTemplate(it.templates())
        }

        commandProvider.register(InfoCommand())
        commandProvider.register(HelpCommand())
        commandProvider.register(ExitCommand())
        commandProvider.register(ClearCommand())
        commandProvider.register(VersionCommand())
        commandProvider.register(TaskCommand())
        commandProvider.register(ServiceCommand())
        commandProvider.register(DebugCommand())

        logger.info(
            translator.trans("node.boot.success.message"),
            System.currentTimeMillis() - System.getProperty("startup").toLong()
        )

        if (!terminal.commandReadingThread.isAlive) {
            terminal.allowInput()
        }

        ServiceStartScheduler.schedule()
        PlayerEventListener
    }

    fun getRC(): RedisController? {
        return redisController
    }
    fun getDB(): MySQLController? {
        return mysqlController
    }

    companion object {
        lateinit var instance: Node
    }
}