package io.github.thecguygithub.node

import io.github.thecguygithub.api.JavaCloudAPI
import io.github.thecguygithub.node.command.CommandProvider
import io.github.thecguygithub.node.terminal.JLineTerminal
import io.github.thecguygithub.node.util.Configurations.readContent
import java.nio.file.Path


abstract class Node: JavaCloudAPI() {

    companion object {
        var instance: Node? = null
            private set

        var nodeConfig: NodeConfig? = null
            private set

        var terminal: JLineTerminal? = null
            private set

        var commandProvider: CommandProvider? = null
            private set
    }

    init {
        instance = this

        nodeConfig = readContent(Path.of("config.json"), NodeConfig());

        terminal = JLineTerminal(nodeConfig!!)

        commandProvider = CommandProvider()

        terminal!!.allowInput()

    }
}