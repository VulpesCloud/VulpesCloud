package de.vulpescloud.node

import de.vulpescloud.api.language.Translator
import de.vulpescloud.node.command.provider.CommandProvider
import de.vulpescloud.node.commands.ClearCommand
import de.vulpescloud.node.commands.ExitCommand
import de.vulpescloud.node.commands.HelpCommand
import de.vulpescloud.node.commands.InfoCommand
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.networking.mysql.MySQLController
import de.vulpescloud.node.networking.redis.RedisController
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setups.FirstSetup
import de.vulpescloud.node.terminal.JLineTerminal
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

        commandProvider.register(InfoCommand())
        commandProvider.register(HelpCommand())
        commandProvider.register(ExitCommand())
        commandProvider.register(ClearCommand())

        logger.info(
            translator.trans("node.boot.success.message"),
            System.currentTimeMillis() - System.getProperty("startup").toLong()
        )

        terminal.allowInput()
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