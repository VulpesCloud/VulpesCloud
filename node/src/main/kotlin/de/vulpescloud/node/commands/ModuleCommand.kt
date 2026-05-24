package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.suggestion.Suggestions

@Suppress("UNUSED")
class ModuleCommand {

    private val moduleProvider = Node.instance.moduleProvider

    @Suggestions("loadedModules")
    fun loadedModulesSuggestions(): Stream<String> {
        return moduleProvider.getAllModules().map { it.moduleInfo.name }.stream()
    }

    @Suggestions("downloadableModules")
    fun downloadableModulesSuggestions(): Stream<String> {
        return moduleProvider.getAllDownloadableModules().map { it.name }.stream()
    }

    @Command("module load <name>")
    fun loadModule(source: CommandSource, @Argument("name") name: String) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        runBlocking {
            val module = moduleProvider.loadModule(name)
            if (module != null) {
                source.sendMessage("<green>Module <yellow>$name <green>was loaded successfully.")
            } else {
                source.sendMessage(
                    "<red>Failed to load module <yellow>$name<red>. Check logs for details."
                )
            }
        }
    }

    @Command("module start <name>")
    fun startModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        val success = moduleProvider.enableModule(name)
        if (success) {
            source.sendMessage("<green>Module <yellow>$name <green>was started successfully.")
        } else {
            source.sendMessage(
                "<red>Failed to start module <yellow>$name<red>. Is it already running or not loaded?"
            )
        }
    }

    @Command("module stop <name>")
    fun stopModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
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
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        val success = moduleProvider.unloadModule(name)
        if (success) {
            source.sendMessage("<green>Module <yellow>$name <green>was unloaded successfully.")
        } else {
            source.sendMessage("<red>Failed to unload module <yellow>$name<red>.")
        }
    }

    @Command("module list")
    fun listModules(source: CommandSource) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
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

    @Command("module list downloadable")
    fun listDownloadableModules(source: CommandSource) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        try {
            runBlocking {
                withTimeout(5.seconds) {
                    val downloadableModules = moduleProvider.getAllDownloadableModules()
                    if (downloadableModules.isEmpty()) {
                        source.sendMessage("<red>No downloadable modules are currently available.")
                        return@withTimeout
                    }
                    source.sendMessage(
                        "<green>Available downloadable modules (<yellow>${downloadableModules.size}<green>):"
                    )
                    downloadableModules.forEach {
                        source.sendMessage(
                            "<dark_gray>- <yellow>${it.name} <dark_gray>(Version: <gold>${it.version}<dark_gray>) Description: ${it.description} Authors: ${it.authors.joinToString()} Website: ${it.website} Support: ${it.supportURL}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            source.sendMessage("<red>Failed to fetch downloadable modules: ${e.message}")
        }
    }

    @Command("module update <name>")
    fun updateModule(
        source: CommandSource,
        @Argument("name", suggestions = "downloadableModules") name: String,
        @Flag("force") force: Boolean,
    ) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        try {
            val module = moduleProvider.getDownloadableModule(name)
            if (module == null) {
                source.sendMessage("<red>Module <yellow>$name <red>not found.")
                return
            }

            if (force) {
                return runBlocking { moduleProvider.forceUpdateModule(module) }
            }

            runBlocking { moduleProvider.updateDownloadableModule(module) }
        } catch (e: Exception) {
            source.sendMessage("<red>Failed to update module: $e")
        }
    }

    @Command("module restart <name>")
    fun reloadModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        runBlocking {
            val success = moduleProvider.restartModule(name)
            if (success) {
                source.sendMessage("<green>Module <yellow>$name <green>was reloaded successfully.")
            } else {
                source.sendMessage("<red>Failed to reload module <yellow>$name<red>.")
            }
        }
    }

    @Command("module info installed <name>")
    fun installedModuleInfo(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        val module = moduleProvider.getModule(name)
        if (module == null) {
            source.sendMessage("<red>Module <yellow>$name <red>not found.")
            return
        }
        val moduleInfo = module.moduleInfo
        source.sendMessage("Module <light_purple>${moduleInfo.name}</light_purple> Stats: ")
        source.sendMessage(
            "<dark_gray>- <white>Version:</white> <yellow>${moduleInfo.version}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>State:</white> <yellow>${moduleInfo.state}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Authors:</white> <yellow>${moduleInfo.authors.joinToString()}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Description:</white> <yellow>${moduleInfo.description}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Website:</white> <yellow>${moduleInfo.website}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Platforms:</white> <yellow>${moduleInfo.platforms.joinToString()}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Update Command:</white> <yellow>'module update ${moduleInfo.name}'</yellow>"
        )
    }

    @Command("module info downloadable <name>")
    fun downloadableModuleInfo(
        source: CommandSource,
        @Argument("name", suggestions = "downloadableModules") name: String,
        @Flag("detailed") detailed: Boolean,
    ) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        val module = moduleProvider.getDownloadableModule(name)
        if (module == null) {
            source.sendMessage("<red>Module <yellow>$name <red>not found.")
            return
        }

        source.sendMessage("Module <light_purple>${module.name}</light_purple> Stats: ")
        source.sendMessage(
            "<dark_gray>- <white>Version:</white> <yellow>${module.version}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Description:</white> <yellow>${module.description}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Authors:</white> <yellow>${module.authors.joinToString()}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Website:</white> <yellow>${module.website}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Support URL:</white> <yellow>${module.supportURL}</yellow>"
        )
        source.sendMessage(
            "<dark_gray>- <white>Install/Update Command:</white> <yellow>'module update ${module.name}'</yellow>"
        )
        if (detailed) {
            source.sendMessage(
                "<dark_gray>- <white>InstallURL:</white> <yellow>${module.installURL}</yellow>"
            )
        }
    }

    @Command("module checkForUpdates")
    fun checkForModuleUpdates(source: CommandSource) {
        if (source !is ConsoleCommandSource) {
            source.sendMessage("<red>This command can only be executed from the node console.")
            return
        }
        moduleProvider.checkAllLoadedModulesForUpdates()
    }
}
