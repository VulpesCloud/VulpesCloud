package de.vulpescloud.node

import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.impl.CommandProviderImpl
import de.vulpescloud.node.commands.ExitCommand
import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.terminal.impl.JLineTerminalImpl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Node : KoinComponent {
    private val nodeModule = module {
        single<JLineTerminal> { JLineTerminalImpl() }
        single<CommandProvider> {CommandProviderImpl()}
    }

    private val terminal: JLineTerminal by inject()
    private val commandProvider: CommandProvider by inject()

    init {
        startKoin {
            modules(nodeModule)
        }
        terminal.initTerminal()

        commandProvider.initialize()

        commandProvider.register(ExitCommand())

        terminal.allowInput()

    }

}