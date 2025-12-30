package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.command.CommandSource
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.suggestion.Suggestions
import java.util.stream.Stream

@Suppress("UNUSED")
class ModuleCommand {

    private val moduleProvider = Node.instance.moduleProvider

    @Suggestions("loadedModules")
    fun loadedModulesSuggestions(): Stream<String> {
        return moduleProvider.getAllModules().map { it.moduleInfo.name }.stream()
    }

    @Command("module load <name>")
    fun loadModule(
        source: CommandSource,
        @Argument("name") name: String
    ) {
        NodeCoroutineScope.launch {
            val module = moduleProvider.loadModule(name)
            if (module != null) {
                source.sendMessage("<green>Module <yellow>$name <green>was loaded successfully.")
            } else {
                source.sendMessage("<red>Failed to load module <yellow>$name<red>. Check logs for details.")
            }
        }
    }

    @Command("module start <name>")
    fun startModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String
    ) {
        val success = moduleProvider.enableModule(name)
        if (success) {
            source.sendMessage("<green>Module <yellow>$name <green>was started successfully.")
        } else {
            source.sendMessage("<red>Failed to start module <yellow>$name<red>. Is it already running or not loaded?")
        }
    }

    @Command("module stop <name>")
    fun stopModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String
    ) {
        val success = moduleProvider.disableModule(name)
        if (success) {
            source.sendMessage("<green>Module <yellow>$name <green>was stopped successfully.")
        } else {
            source.sendMessage("<red>Failed to stop module <yellow>$name<red>. Is it even running?")
        }
    }

    @Command("module unload <name>")
    fun unloadModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String
    ) {
        val success = moduleProvider.unloadModule(name)
        if (success) {
            source.sendMessage("<green>Module <yellow>$name <green>was unloaded successfully.")
        } else {
            source.sendMessage("<red>Failed to unload module <yellow>$name<red>.")
        }
    }

    @Command("module list")
    fun listModules(source: CommandSource) {
        val modules = moduleProvider.getAllModules()
        if (modules.isEmpty()) {
            source.sendMessage("<red>No modules are currently loaded.")
            return
        }

        source.sendMessage("<green>Loaded modules (<yellow>${modules.size}<green>):")
        modules.forEach {
            val statusColor = if (it.moduleInfo.state.name == "ENABLED") "<green>" else "<red>"
            source.sendMessage(
                "<dark_gray>- <yellow>${it.moduleInfo.name} <dark_gray>(Version: <gold>${it.moduleInfo.version}<dark_gray>) Status: $statusColor${it.moduleInfo.state}"
            )
        }
    }

    @Command("module restart <name>")
    fun reloadModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String
    ) {
        NodeCoroutineScope.launch {
            val success = moduleProvider.restartModule(name)
            if (success) {
                source.sendMessage("<green>Module <yellow>$name <green>was reloaded successfully.")
            } else {
                source.sendMessage("<red>Failed to reload module <yellow>$name<red>.")
            }
        }
    }
}
