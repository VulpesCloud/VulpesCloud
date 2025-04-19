package de.vulpescloud.node.commands

import de.vulpescloud.api.lang.Translator
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.setups.TaskSetup
import de.vulpescloud.node.terminal.JLineTerminal
import org.incendo.cloud.annotations.Command

@Suppress("Unused")
class TaskCommand(
    val setupProvider: SetupProvider,
    val taskProvider: TaskProvider,
    val translator: Translator,
    val terminal: JLineTerminal,
    val config: NodeConfig,
    val versionProvider: VersionProvider,
) {

    @Command("task|tasks create")
    fun startSetup() {
        setupProvider.startSetup(
            TaskSetup(taskProvider, translator, terminal, versionProvider, config)
        )
    }
}
