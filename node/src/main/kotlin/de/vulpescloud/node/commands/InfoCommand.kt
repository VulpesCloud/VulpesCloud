package de.vulpescloud.node.commands

import de.vulpescloud.launcher.VulpesLauncher
import de.vulpescloud.node.CloudVersion
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import de.vulpescloud.node.command.annotation.SpecificCommandSource
import org.incendo.cloud.annotations.Command
import java.lang.management.ManagementFactory

@SpecificCommandSource(ConsoleCommandSource::class)
class InfoCommand {

    private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()

    @Command("info|i")
    fun displayInfo(source: CommandSource) {
        source.sendMessage("<gray>Operating System<dark_gray>:</dark_gray> <white>${System.getProperty("os.name")}</white>")
        source.sendMessage("<gray>Memory Usage<dark_gray>:</dark_gray> <white>${usedMemory()}</white>")
        source.sendMessage(
            "<gray>Java<dark_gray>:</dark_gray> <white>${runtimeMXBean.vmVendor} ${runtimeMXBean.specVersion}</white> <dark_gray>[</dark_gray> <white>${runtimeMXBean.vmName} ${runtimeMXBean.vmVersion}</white> <dark_gray>]</dark_gray>"
        )
        source.sendMessage("<gold>-----------------------------------------------------</gold>")
        source.sendMessage("<gray>Update Branch<dark_gray>:</dark_gray> <white>${VulpesLauncher.config.autoUpdatesBranch()}</white>")
        source.sendMessage("<gray>Auto-Updates<dark_gray>:</dark_gray> <white>${VulpesLauncher.config.autoUpdatesEnabled()}</white>")
        source.sendMessage("<gray>Version<dark_gray>:</dark_gray> <white>${CloudVersion.getFullVersion()}</white>")
        source.sendMessage("<gold>------------------------------------------------------</gold>")
        source.sendMessage(
            "<gray>Uptime<dark_gray>:</dark_gray> <white>${(System.currentTimeMillis() - System.getProperty("startup").toLong()) / 1000}s</white>"
        )
    }

    private fun usedMemory(): String {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
                1024 /
                1024)
            .toString() + " mb"
    }
}
