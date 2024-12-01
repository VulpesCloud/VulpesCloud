package de.vulpescloud.node

import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.terminal.JLineTerminal

class Node {
    val config = ConfigProvider()
    val terminal = JLineTerminal()

    init {
        instance = this
        terminal.initialize()

        terminal.allowInput()
    }

    companion object {
        lateinit var instance: Node
    }
}