package de.vulpescloud.node.commands

import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.module.ModuleProvider
import org.incendo.cloud.annotations.Argument
import java.util.stream.Stream
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.path.name

@Suppress("Unused")
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
            moduleProvider.moduleFolder().find { it.name == command } ?: throw IllegalArgumentException()

        return path.toFile()
    }

    @Suggestions("modulesPath")
    fun suggestModuleFiles(): List<String> {
        return moduleProvider.moduleFolder().toFile().listFiles()?.filter { it.extension == "jar" }?.map { it.name } ?: emptyList()
    }

    @Command("module|modules list")
    fun listModules(source: CommandSource) {
        source.sendMessage("A total of ${moduleProvider.modules().size}")
        moduleProvider.modules().forEach {
            source.sendMessage(
                " &8- &m${it.name} &7State&8: &e${it.state} &7Authors&8: &e${it.authors} &7Version&8: &e${it.version} &7Description&8: &e${it.description}"
            )
        }
    }

    @Command("module|modules load <moduleFile>")
    fun loadModule(
        source: CommandSource,
        @Argument("moduleFile", parserName = "modulesPath") file: File
    ) {
        source.sendMessage("Trying to load the Module!")
        moduleProvider.loadModule(file)
    }

    @Command("module|modules unload <module>")
    fun unloadModule(
        source: CommandSource,
        @Argument("module", parserName = "modules") module: ModuleInfo
    ) {
        source.sendMessage("Trying to unload the Module!")
        moduleProvider.unloadModule(module.name)
    }
}
