package io.github.thecguygithub.node.command

interface CommandService {

    fun commands(): List<Command?>?

    fun commandsByName(name: String?): List<Command?>?

    fun registerCommand(command: Command?)

    // fun registerCommands(vararg command: Command?)

    fun unregisterCommand(command: Command?)

    fun call(commandId: String?, args: Array<String?>?)

    fun registerCommands(vararg commands: Command)
}