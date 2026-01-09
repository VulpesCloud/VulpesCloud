package de.vulpescloud.node.command

interface CommandSource {

    fun sendMessage(message: String)

    companion object {
        val CONSOLE = ConsoleCommandSource()
    }

}
