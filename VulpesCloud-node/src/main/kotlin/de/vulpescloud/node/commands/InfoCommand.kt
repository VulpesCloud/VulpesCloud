package de.vulpescloud.node.commands

import de.vulpescloud.node.command.source.CommandSource
import org.incendo.cloud.annotations.Command
import java.lang.management.ManagementFactory


class InfoCommand {

    val runtimeMXBean = ManagementFactory.getRuntimeMXBean()


    @Command("info")
    fun infoCommand(
        sender: CommandSource
    ) {
        sender.sendMessage("Operating System&8: &f${System.getProperty("os.name")}")
        sender.sendMessage("Used Memory of the Node process&8: &f${usedMemory()}")
        sender.sendMessage("Java&8: &f${runtimeMXBean.vmVendor} ${runtimeMXBean.specVersion} &8[&f ${runtimeMXBean.vmName} ${runtimeMXBean.vmVersion} &8]")
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
            .freeMemory()) / 1024 / 1024).toString() + " mb"
    }

}