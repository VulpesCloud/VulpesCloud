package de.vulpescloud.node

import de.vulpescloud.api.language.Translator
import de.vulpescloud.node.command.provider.CommandProvider
import de.vulpescloud.node.commands.ClearCommand
import de.vulpescloud.node.commands.ExitCommand
import de.vulpescloud.node.commands.HelpCommand
import de.vulpescloud.node.commands.InfoCommand
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setups.FirstSetup
import de.vulpescloud.node.terminal.JLineTerminal
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

        commandProvider.register(InfoCommand())
        commandProvider.register(HelpCommand())
        commandProvider.register(ExitCommand())
        commandProvider.register(ClearCommand())

        terminal.allowInput()
    }

    companion object {
        lateinit var instance: Node
    }
}