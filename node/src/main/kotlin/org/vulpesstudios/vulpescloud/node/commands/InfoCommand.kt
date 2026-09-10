/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.commands

import org.incendo.cloud.annotations.Command
import org.vulpesstudios.vulpescloud.launcher.VulpesLauncher
import org.vulpesstudios.vulpescloud.node.CloudVersion
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import org.vulpesstudios.vulpescloud.node.command.ConsoleCommandSource
import org.vulpesstudios.vulpescloud.node.command.annotation.SpecificCommandSource
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
