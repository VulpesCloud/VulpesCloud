package de.vulpescloud.node.commands

import de.vulpescloud.api.modules.DownloadableModule
import de.vulpescloud.api.modules.ModuleInfo
import de.vulpescloud.api.modules.ModuleStates
import de.vulpescloud.launcher.util.FileUpdaterUtil
import de.vulpescloud.node.Node
import de.vulpescloud.node.command.annotations.Alias
import de.vulpescloud.node.command.source.CommandSource
import de.vulpescloud.node.json.ModuleSerializer.getModuleInfoFromJson
import de.vulpescloud.node.modules.LoadedModule
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandInput
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URI
import java.net.URLClassLoader
import java.util.jar.JarFile
import java.util.stream.Stream
import kotlin.io.path.Path

@Alias(["modules"])
@Suppress("unused")
class ModuleCommand {

    @Parser(suggestions = "modules", name = "modules")
    fun moduleParser(input: CommandInput): ModuleInfo {
        val command = input.readString()
        val module =
            Node.instance.moduleProvider.modules.find { it.name == command } ?: throw IllegalArgumentException()

        return module
    }

    @Suggestions("modules")
    fun suggestModules(): Stream<String> {
        return Node.instance.moduleProvider.modules.stream().map { it.name }
    }


    @Parser(suggestions = "downloadables", name = "downloadables")
    fun downloadableModuleParser(input: CommandInput): DownloadableModule {
        val command = input.readString()
        val module = Node.instance.moduleProvider.downloadableModules.find { it.name == command }
            ?: throw IllegalArgumentException()

        return module
    }

    @Suggestions("downloadables")
    fun suggestDownloadableModules(): Stream<String> {
        return Node.instance.moduleProvider.downloadableModules.stream().map { it.name }
    }


    @Command("module|modules list")
    fun listModules(source: CommandSource) {
        Node.instance.moduleProvider.modules.forEach {
            source.sendMessage(" &8- &m${it.name} &8| &f${it.state} &8| &b${it.description}")
        }
    }

    @Command("module|modules stop <module>")
    fun stopModule(
        source: CommandSource,
        @Argument("module", parserName = "modules") moduleInfo: ModuleInfo,
    ) {
        val loadedModule = Node.instance.moduleProvider.loadedModules.find { it.moduleInfo.name == moduleInfo.name }
        if (loadedModule != null) {
            if (loadedModule.moduleInfo.state == ModuleStates.STARTED) {
                Node.instance.moduleProvider.stopModule(loadedModule)
            } else {
                source.sendMessage("Module &m${moduleInfo.name}&f is not started.")
            }
        } else {
            source.sendMessage("Module &m${moduleInfo.name}&f is not loaded.")
        }
    }

    @Command("module|modules start <module>")
    fun startModule(
        source: CommandSource,
        @Argument("module", parserName = "modules") moduleInfo: ModuleInfo,
    ) {
        val loadedModule = Node.instance.moduleProvider.loadedModules.find { it.moduleInfo.name == moduleInfo.name }
        if (loadedModule != null) {
            if (loadedModule.moduleInfo.state != ModuleStates.STARTED) {
                Node.instance.moduleProvider.startModule(loadedModule)
            } else {
                source.sendMessage("Module &m${moduleInfo.name}&f is already started.")
            }
        } else {
            source.sendMessage("Module &m${moduleInfo.name}&f is not loaded.")
        }
    }

    @Command("module|modules install")
    fun showInstallableModules(
        source: CommandSource,
    ) {
        Node.instance.moduleProvider.downloadableModules.forEach {
            source.sendMessage(" &8- &m${it.name} &8| &f${it.version} &8| &b${it.description} &8| &f${it.supportURL} &8| &f${it.authors}")
        }
    }

    @Command("module|modules install <downloadable>")
    fun installModules(
        source: CommandSource,
        @Argument("downloadable", parserName = "downloadables") downloadableModule: DownloadableModule,
    ) {
        if (Node.instance.moduleProvider.modules.find { it.name == downloadableModule.name } == null) {
            source.sendMessage("Trying to install Module &m${downloadableModule.name}")

            val loadedModule: LoadedModule

            FileUpdaterUtil.get(
                URI(downloadableModule.installURL),
                FileUpdaterUtil.filePathHandler(Path("modules/${downloadableModule.name}.jar"))
            )

            val jarFile = JarFile(Path("modules/${downloadableModule.name}.jar").toFile())

            val moduleJson = jarFile.getJarEntry("module.json")
            if (moduleJson == null) {
                source.sendMessage("Jar File &b${jarFile.name} does not contain a module.json and is in the modules folder!")
                return
            }
            val reader = InputStreamReader(jarFile.getInputStream(moduleJson))
            val json = JSONObject(reader.readText())
            val moduleInfo = getModuleInfoFromJson(json)
            if (moduleInfo == null) {
                source.sendMessage("Module ${jarFile.name} contains an invalid module.json")
                return
            }
            source.sendMessage("Loading Module &m${moduleInfo.name}")
            try {
                val vulpesModule = Node.instance.moduleProvider.loadModule(
                    Path("modules/${downloadableModule.name}.jar").toFile(),
                    moduleInfo.main
                )
                val classLoader = vulpesModule.javaClass.classLoader as URLClassLoader
                loadedModule = LoadedModule(vulpesModule, classLoader, moduleInfo)
                loadedModule.moduleInfo.state = ModuleStates.LOADED
                Node.instance.moduleProvider.modules.add(moduleInfo)
                Node.instance.moduleProvider.loadedModules.add(loadedModule)

            } catch (e: Exception) {
                source.sendMessage("Failed to load Module {}", moduleInfo.name)
                e.printStackTrace()
                return
            }

            source.sendMessage("Starting Module &m${loadedModule.moduleInfo.name}")
            loadedModule.module.enable()
            loadedModule.moduleInfo.state = ModuleStates.STARTED
        }
    }

}