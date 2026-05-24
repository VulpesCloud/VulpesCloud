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
        source.sendMessage("Operating System<dark_gray>: <white>${System.getProperty("os.name")}")
        source.sendMessage("Used Memory of the Node process<dark_gray>: <white>${usedMemory()}")
        source.sendMessage(
            "Java<dark_gray>: <white>${runtimeMXBean.vmVendor} ${runtimeMXBean.specVersion} <dark_gray>[<white> ${runtimeMXBean.vmName} ${runtimeMXBean.vmVersion} <dark_gray>]"
        )
        source.sendMessage("-----------------------------------------------------")
        source.sendMessage("Update Branch<dark_gray>: <white>${VulpesLauncher.config.autoUpdatesBranch()}")
        source.sendMessage("AutoUpdates Enabled<dark_gray>: <white>${VulpesLauncher.config.autoUpdatesEnabled()}")
        source.sendMessage("Version<dark_gray>: <white>${CloudVersion.getFullVersion()}")
        source.sendMessage("------------------------------------------------------")
        source.sendMessage(
            "Uptime<dark_gray>: <white>${(System.currentTimeMillis() - System.getProperty("startup").toLong()) / 1000 } seconds"
        )
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
                1024 /
                1024)
            .toString() + " mb"
    }
}
