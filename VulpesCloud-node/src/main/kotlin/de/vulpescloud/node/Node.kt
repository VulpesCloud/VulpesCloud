package de.vulpescloud.node

import de.vulpescloud.api.event.annotations.EventHandler
import de.vulpescloud.api.language.Translator
import de.vulpescloud.api.utils.StringUtils
import de.vulpescloud.node.command.provider.CommandProvider
import de.vulpescloud.node.commands.*
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.event.EventManagerImpl
import de.vulpescloud.node.event.events.testEvent
import de.vulpescloud.node.manager.AuthenticationManager
import de.vulpescloud.node.modules.ModuleProvider
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val moduleProvider = ModuleProvider()
    val authenticationManager = AuthenticationManager()
    val eventManager = EventManagerImpl()

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

        authenticationManager.initializeAuth()
        authenticationManager.sendAuthentication()

        versionProvider.initialize()

        taskProvider.tasks().forEach {
            logger.debug("Preparing a template for task &m${it.name()}")
            templateProvider.prepareTemplate(it.templates())
        }

        moduleProvider.loadAllModules()

        commandProvider.register(InfoCommand())
        commandProvider.register(HelpCommand())
        commandProvider.register(ExitCommand())
        commandProvider.register(ClearCommand())
        commandProvider.register(VersionCommand())
        commandProvider.register(TaskCommand())
        commandProvider.register(ServiceCommand())
        commandProvider.register(DebugCommand())
        commandProvider.register(ModuleCommand())


        serviceProvider.initializeSub()

        logger.info(
            translator.trans("node.boot.success.message"),
            System.currentTimeMillis() - System.getProperty("startup").toLong()
        )

        if (!terminal.commandReadingThread.isAlive) {
            terminal.allowInput()
        }

        ServiceStartScheduler.schedule()
        PlayerEventListener

        logger.debug("Registering Event")

        eventManager.registerListener(this::class)

        GlobalScope.launch {
            logger.debug("Calling Event")
            delay(1999)
            eventManager.call(testEvent)
        }
    }

    val test = eventManager.listen<testEvent> {
        logger.debug("KTNODE> event triggered ${it.text}")
    }

    @EventHandler
    fun onTestEvent(event: testEvent) {
        logger.debug("NODE> event triggered ${event.text}")
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