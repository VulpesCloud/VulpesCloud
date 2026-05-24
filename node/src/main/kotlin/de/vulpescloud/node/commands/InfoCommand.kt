package de.vulpescloud.node.commands

import de.vulpescloud.launcher.VulpesLauncher
import de.vulpescloud.node.CloudVersion
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import org.incendo.cloud.annotations.Command
import java.lang.management.ManagementFactory

class InfoCommand {

    private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()

    @Command("info|i")
    fun displayInfo(source: CommandSource) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        source.sendMessage("Operating System&8: &f${System.getProperty("os.name")}")
        source.sendMessage("Used Memory of the Node process&8: &f${usedMemory()}")
        source.sendMessage(
            "Java&8: &f${runtimeMXBean.vmVendor} ${runtimeMXBean.specVersion} &8[&f ${runtimeMXBean.vmName} ${runtimeMXBean.vmVersion} &8]"
        )
        source.sendMessage("-----------------------------------------------------")
        source.sendMessage("Update Branch&8: &f${VulpesLauncher.config.autoUpdatesBranch()}")
        source.sendMessage("AutoUpdates Enabled&8: &f${VulpesLauncher.config.autoUpdatesEnabled()}")
        source.sendMessage("Version&8: &f${CloudVersion.getFullVersion()}")
        source.sendMessage("------------------------------------------------------")
        source.sendMessage(
            "Uptime&8: &f${(System.currentTimeMillis() - System.getProperty("startup").toLong()) / 1000 } seconds"
        )
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
                1024 /
                1024)
            .toString() + " mb"
    }
}
