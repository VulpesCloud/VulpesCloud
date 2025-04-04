package de.vulpescloud.node

import de.vulpescloud.node.terminal.JLineTerminal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

object NodeShutdown : KoinComponent {

    private val terminal: JLineTerminal by inject()

    fun ctrlCCloud() {
        terminal.close()
        exitProcess(0)
    }

}