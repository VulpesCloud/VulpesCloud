package de.vulpescloud.node

import de.vulpescloud.node.command.provider.CommandProvider
import de.vulpescloud.node.commands.HelpCommand
import de.vulpescloud.node.commands.InfoCommand
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.terminal.JLineTerminal

class Node {
    val config = ConfigProvider()
    val terminal = JLineTerminal()
    val commandProvider = CommandProvider()

    init {
        instance = this
        terminal.initialize()

        commandProvider.register(InfoCommand())
        commandProvider.register(HelpCommand())

        terminal.allowInput()
    }

    companion object {
        lateinit var instance: Node
    }
}