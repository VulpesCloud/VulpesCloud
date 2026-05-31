package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import de.vulpescloud.node.command.annotation.SpecificCommandSource
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.suggestion.Suggestions

@Suppress("UNUSED")
@SpecificCommandSource(ConsoleCommandSource::class)
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
        runBlocking {
            val module = moduleProvider.loadModule(name)
            if (module != null) {
                source.sendMessage("<green>Module</green> <white>$name</white> <green>was loaded successfully.</green>")
            } else {
                source.sendMessage(
                    "<red>Failed to load module</red> <white>$name</white><red>. Check logs for details.</red>"
                )
            }
        }
    }

    @Command("module start <name>")
    fun startModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        val success = moduleProvider.enableModule(name)
        if (success) {
            source.sendMessage("<green>Module</green> <white>$name</white> <green>was started successfully.</green>")
        } else {
            source.sendMessage(
                "<red>Failed to start module</red> <white>$name</white><red>. Is it already running or not loaded?</red>"
            )
        }
    }

    @Command("module stop <name>")
    fun stopModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        val success = moduleProvider.disableModule(name)
        if (success) {
            source.sendMessage("<green>Module</green> <white>$name</white> <green>was stopped successfully.</green>")
        } else {
            source.sendMessage("<red>Failed to stop module</red> <white>$name</white><red>. Is it even running?</red>")
        }
    }

    @Command("module unload <name>")
    fun unloadModule(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        val success = moduleProvider.unloadModule(name)
        if (success) {
            source.sendMessage("<green>Module</green> <white>$name</white> <green>was unloaded successfully.</green>")
        } else {
            source.sendMessage("<red>Failed to unload module</red> <white>$name</white><red>.</red>")
        }
    }

    @Command("module list")
    fun listModules(source: CommandSource) {
        val modules = moduleProvider.getAllModules()
        if (modules.isEmpty()) {
            source.sendMessage("<red>No modules are currently loaded.")
            return
        }

        source.sendMessage("<gray>Loaded modules (<gold>${modules.size}</gold>):</gray>")
        modules.forEach {
            val statusColor = if (it.moduleInfo.state.name == "ENABLED") "<green>" else "<red>"
            source.sendMessage(
                " <dark_gray>»</dark_gray> <white>${it.moduleInfo.name}</white> <dark_gray>| <gray>Version:</gray> <white>${it.moduleInfo.version}</white> <dark_gray>| <gray>Status:</gray> $statusColor${it.moduleInfo.state}"
            )
        }
    }

    @Command("module list downloadable")
    fun listDownloadableModules(source: CommandSource) {
        try {
            runBlocking {
                withTimeout(5.seconds) {
                    val downloadableModules = moduleProvider.getAllDownloadableModules()
                    if (downloadableModules.isEmpty()) {
                        source.sendMessage("<red>No downloadable modules are currently available.")
                        return@withTimeout
                    }
                    source.sendMessage(
                        "<gray>Available downloadable modules (<gold>${downloadableModules.size}</gold>):</gray>"
                    )
                    downloadableModules.forEach {
                        source.sendMessage(
                            " <dark_gray>»</dark_gray> <white>${it.name}</white> <dark_gray>| <gray>Version:</gray> <white>${it.version}</white> <dark_gray>| <gray>Description:</gray> <white>${it.description}</white>"
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
        try {
            val module = moduleProvider.getDownloadableModule(name)
            if (module == null) {
                source.sendMessage("<red>Module</red> <white>$name</white> <red>not found.</red>")
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
        runBlocking {
            val success = moduleProvider.restartModule(name)
            if (success) {
                source.sendMessage("<green>Module</green> <white>$name</white> <green>was reloaded successfully.</green>")
            } else {
                source.sendMessage("<red>Failed to reload module</red> <white>$name</white><red>.</red>")
            }
        }
    }

    @Command("module info installed <name>")
    fun installedModuleInfo(
        source: CommandSource,
        @Argument("name", suggestions = "loadedModules") name: String,
    ) {
        val module = moduleProvider.getModule(name)
        if (module == null) {
            source.sendMessage("<red>Module</red> <white>$name</white> <red>not found.</red>")
            return
        }
        val moduleInfo = module.moduleInfo
        source.sendMessage("<gold>---------</gold> <white>${moduleInfo.name}</white> <gold>---------</gold>")
        source.sendMessage("<gray>Version<dark_gray>:</dark_gray> <white>${moduleInfo.version}</white>")
        source.sendMessage("<gray>State<dark_gray>:</dark_gray> <white>${moduleInfo.state}</white>")
        source.sendMessage("<gray>Authors<dark_gray>:</dark_gray> <white>${moduleInfo.authors.joinToString()}</white>")
        source.sendMessage("<gray>Description<dark_gray>:</dark_gray> <white>${moduleInfo.description}</white>")
        source.sendMessage("<gray>Website<dark_gray>:</dark_gray> <white>${moduleInfo.website}</white>")
        source.sendMessage("<gray>Platforms<dark_gray>:</dark_gray> <white>${moduleInfo.platforms.joinToString()}</white>")
        source.sendMessage(
            "<gray>Update Command<dark_gray>:</dark_gray> <white>'module update ${moduleInfo.name}'</white>"
        )
    }

    @Command("module info downloadable <name>")
    fun downloadableModuleInfo(
        source: CommandSource,
        @Argument("name", suggestions = "downloadableModules") name: String,
        @Flag("detailed") detailed: Boolean,
    ) {
        val module = moduleProvider.getDownloadableModule(name)
        if (module == null) {
            source.sendMessage("<red>Module</red> <white>$name</white> <red>not found.</red>")
            return
        }

        source.sendMessage("<gold>---------</gold> <white>${module.name}</white> <gold>---------</gold>")
        source.sendMessage("<gray>Version<dark_gray>:</dark_gray> <white>${module.version}</white>")
        source.sendMessage("<gray>Description<dark_gray>:</dark_gray> <white>${module.description}</white>")
        source.sendMessage("<gray>Authors<dark_gray>:</dark_gray> <white>${module.authors.joinToString()}</white>")
        source.sendMessage("<gray>Website<dark_gray>:</dark_gray> <white>${module.website}</white>")
        source.sendMessage("<gray>Support URL<dark_gray>:</dark_gray> <white>${module.supportURL}</white>")
        source.sendMessage(
            "<gray>Install/Update Command<dark_gray>:</dark_gray> <white>'module update ${module.name}'</white>"
        )
        if (detailed) {
            source.sendMessage(
                "<gray>Install URL<dark_gray>:</dark_gray> <white>${module.installURL}</white>"
            )
        }
    }

    @Command("module checkForUpdates")
    fun checkForModuleUpdates(source: CommandSource) {
        moduleProvider.checkAllLoadedModulesForUpdates()
    }
}
