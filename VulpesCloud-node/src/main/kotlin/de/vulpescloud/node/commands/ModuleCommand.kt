package de.vulpescloud.node.commands

import de.vulpescloud.api.module.DownloadableModule
import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.launcher.util.FileUpdaterUtil
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.annotations.Description
import de.vulpescloud.node.module.ModuleProvider
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.io.path.name

@Suppress("Unused")
@Description("COMMANDS.DESCRIPTION.module")
class ModuleCommand(private val moduleProvider: ModuleProvider) {

    private val logger = LoggerFactory.getLogger("ModuleCommand")

    @Parser(suggestions = "modules", name = "modules")
    fun moduleParser(input: CommandInput): ModuleInfo {
        val command = input.readString()
        val module =
            moduleProvider.modules().find { it.name == command } ?: throw IllegalArgumentException()

        return module
    }

    @Suggestions("modules")
    fun suggestModules(): Stream<String> {
        return moduleProvider.modules().stream().map { it.name }
    }

    @Parser(suggestions = "modulesPath", name = "modulesPath")
    fun parseModuleFiles(input: CommandInput): File {
        val command = input.readString()
        logger.debug(command + " ddd " + moduleProvider.moduleFolder().find { it.name == command })
        val path =
            moduleProvider.moduleFolder().find { it.name == command }
                ?: throw IllegalArgumentException()

        return path.toFile()
    }

    @Suggestions("modulesPath")
    fun suggestModuleFiles(): List<String> {
        return moduleProvider
            .moduleFolder()
            .toFile()
            .listFiles()
            ?.filter { it.extension == "jar" }
            ?.map { it.name } ?: emptyList()
    }

    @Parser(suggestions = "downloadables", name = "downloadables")
    fun downloadableModuleParser(input: CommandInput): DownloadableModule {
        val command = input.readString()
        val module =
            moduleProvider.downloadableModules().find { it.name == command }
                ?: throw IllegalArgumentException()

        return module
    }

    @Suggestions("downloadables")
    fun suggestDownloadableModules(): Stream<String> {
        return moduleProvider.downloadableModules().stream().map { it.name }
    }

    @Command("module|modules list")
    fun listModules(source: CommandSource) {
        source.sendMessage("A total of ${moduleProvider.modules().size} module(s) are loaded:")
        moduleProvider.modules().forEach {
            source.sendMessage(
                " &8- &m${it.name} &7State&8: &e${it.state} &7Authors&8: &e${it.authors} &7Version&8: &e${it.version} &7Description&8: &e${it.description}"
            )
        }
    }

    @Command("module|modules load <moduleFile>")
    fun loadModule(
        source: CommandSource,
        @Argument("moduleFile", parserName = "modulesPath") file: File,
    ) {
        source.sendMessage("Trying to load the Module!")
        moduleProvider.loadModule(file)
    }

    @Command("module|modules unload <module>")
    fun unloadModule(
        source: CommandSource,
        @Argument("module", parserName = "modules") module: ModuleInfo,
    ) {
        source.sendMessage("Trying to unload the Module!")
        moduleProvider.unloadModule(module.name)
    }

    @Command("module|modules install")
    fun showInstallableModules(source: CommandSource) {
        moduleProvider.downloadableModules().forEach {
            source.sendMessage(
                " &8- &m${it.name} &8| &f${it.version} &8| &b${it.description} &8| &f${it.supportURL} &8| &f${it.authors}"
            )
        }
    }

    @Command("module|modules install <downloadable>")
    fun installModules(
        source: CommandSource,
        @Argument("downloadable", parserName = "downloadables")
        downloadableModule: DownloadableModule,
    ) {
        if (moduleProvider.modules().find { it.name == downloadableModule.name } == null) {
            source.sendMessage("Trying to download Module &m${downloadableModule.name}")

            FileUpdaterUtil.get(
                URI(downloadableModule.installURL),
                FileUpdaterUtil.filePathHandler(Path("modules/${downloadableModule.name}.jar")),
            )

            source.sendMessage("Trying to load Module &m${downloadableModule.name}")

            moduleProvider
                .loadModule(Path("modules/${downloadableModule.name}.jar").toFile())
                ?.let { moduleProvider.startModule(it) }
        } else {
            source.sendMessage("Module is already installed!")
        }
    }

    @Command("module|modules uninstall <module>")
    fun uninstallModule(
        source: CommandSource,
        @Argument("module", parserName = "modules") module: ModuleInfo,
    ) {
        source.sendMessage("Trying to unload the Module!")
        moduleProvider.unloadModule(module.name)
        source.sendMessage("Trying to delete the Module!")
        Files.delete(Path("modules/${module.name}.jar"))
    }

    @Command("module|modules update <module>")
    fun updateModule(
        source: CommandSource,
        @Argument("module", parserName = "modules") module: ModuleInfo,
        @Flag("url") urlString: String?,
    ) {
        try {
            source.sendMessage("Trying to update the Module!")
            val uri: String? =
                urlString
                    ?: moduleProvider
                        .downloadableModules()
                        .find { it.name == module.name }
                        ?.installURL

            if (uri == null) {
                source.sendMessage("Cannot update Module &m${module.name} because there is no URL!")
                return
            }

            moduleProvider.unloadModule(module.name)

            Files.delete(Path("modules/${module.name}.jar"))

            source.sendMessage("Trying to download Module &m${module.name}")

            FileUpdaterUtil.get(
                URI(uri),
                FileUpdaterUtil.filePathHandler(Path("modules/${module.name}.jar")),
            )

            source.sendMessage("Trying to load Module &m${module.name}")

            moduleProvider.loadModule(Path("modules/${module.name}.jar").toFile())
        } catch (e: Exception) {
            source.sendMessage(
                "Failed to update Module &m${module.name} because of ${e.stackTraceToString()}"
            )
        }
    }
}
