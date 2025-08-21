package de.vulpescloud.node

import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.commands.ClearCommand
import de.vulpescloud.node.commands.ExitCommand
import de.vulpescloud.node.terminal.Terminal

class Node {

    var terminal: Terminal = Terminal()
    var commandProvider: CommandProvider = CommandProvider()

    init {
        instance = this

        terminal.init()

        commandProvider.initialize()
        commandProvider.apply {
            register(ClearCommand(terminal))
            register(ExitCommand())
        }

        terminal.allowInput()
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Node()
        }

        lateinit var instance: Node
    }

}
