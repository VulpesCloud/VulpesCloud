package de.vulpescloud.node.commands

import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Command


class InfoCommand {

    @Command("info")
    fun infoCommand(
        sender: CommandSource
    ) {
        sender.sendMessage("Java version&8: &f${System.getProperty("java.version")}")
        sender.sendMessage("Operating System&8: &f${System.getProperty("os.name")}")
        sender.sendMessage("Used Memory of the Node process&8: &f${usedMemory()}")
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
            .freeMemory()) / 1024 / 1024).toString() + " mb"
    }

}