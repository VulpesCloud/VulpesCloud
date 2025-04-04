package de.vulpescloud.node

import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.terminal.impl.JLineTerminalImpl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Node : KoinComponent {
    private val nodeModule = module {
        single<JLineTerminal> { JLineTerminalImpl() }
    }

    private val terminal: JLineTerminal by inject()

    init {
        startKoin {
            modules(nodeModule)
        }
        terminal.initTerminal()

        terminal.allowInput()

    }

}