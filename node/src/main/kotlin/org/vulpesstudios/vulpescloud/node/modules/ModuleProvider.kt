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

package org.vulpesstudios.vulpescloud.node.modules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.launcher.VulpesLauncher
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.jar.JarFile

class ModuleProvider(val moduleFolder: Path, val modulesJsonURL: String) {

    private val loadedModules = CopyOnWriteArrayList<LoadedModule>()
    private val classLoaders = ConcurrentHashMap<String, ModuleClassLoader>()

    private val logger = LoggerFactory.getLogger("ModuleProvider")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    init {
        moduleFolder.toFile().mkdirs()
    }

    fun loadModule(name: String): LoadedModule? {
        try {
            moduleFolder.resolve("$name.jar").toFile().let {
                logger.debug("Loading module $name")
                val jarFile = JarFile(it)
                val moduleJsonString =
                    jarFile
                        .getInputStream(jarFile.getJarEntry("module.json"))
                        .readBytes()
                        .decodeToString()
                val moduleInfo = json.decodeFromString<ModuleInfo>(moduleJsonString)
                moduleInfo.state = ModuleStates.LOADED

                val module = loadedModules.find { modu -> modu.moduleInfo.name == moduleInfo.name }
                if (module != null) {
                    return module
                }

                val classLoader =
                    ModuleClassLoader(
                        arrayOf(it.toURI().toURL()),
                        VulpesLauncher.CLASS_LOADER,
                        classLoaders,
                    )
                val clazz = classLoader.loadClass(moduleInfo.main)
                if (!VulpesModule::class.java.isAssignableFrom(clazz)) {
                    logger.warn("Class ${moduleInfo.main} does not implement VulpesModule")
                    return null
                }
                val vulpesModule = clazz.getDeclaredConstructor().newInstance() as VulpesModule
                val loadedModule = LoadedModule(vulpesModule, classLoader, moduleInfo)

                loadedModules.add(loadedModule)
                classLoaders[moduleInfo.name] = classLoader
                // TODO: Module Load Event
                vulpesModule.onLoad()
                logger.info(
                    "Module <light_purple>${moduleInfo.name}</light_purple> has been loaded <green>successfully</green>!"
                )
                return loadedModule
            }
        } catch (exception: Exception) {
            logger.error(
                "An error occurred while loading module $name",
                exception.stackTraceToString(),
            )
            return null
        }
    }

    fun unloadModule(name: String): Boolean {
        val module = loadedModules.find { modul -> modul.moduleInfo.name == name } ?: return false
        return unloadModule(module)
    }

    fun unloadModule(module: LoadedModule): Boolean {
        try {
            if (module.moduleInfo.state == ModuleStates.LOADED) {
                module.module.onUnload()

                module.classLoader.close()
                classLoaders.remove(module.moduleInfo.name)
                loadedModules.remove(module)

                logger.info(
                    "Module <light_purple>${module.moduleInfo.name}</light_purple> has been unloaded <green>successfully</green>!"
                )

                return true
            }
        } catch (exception: Exception) {
            logger.error(
                "An error occurred while unloading module ${module.moduleInfo.name}",
                exception.stackTraceToString(),
            )
            return false
        }

        return false
    }

    fun enableModule(name: String): Boolean {
        val module = loadedModules.find { module -> module.moduleInfo.name == name } ?: return false
        return enableModule(module)
    }

    fun enableModule(module: LoadedModule): Boolean {
        try {
            if (module.moduleInfo.state == ModuleStates.LOADED) {
                module.module.onEnable()
                module.moduleInfo.state = ModuleStates.ENABLED
                logger.info(
                    "Module <light_purple>${module.moduleInfo.name}</light_purple> has been enabled <green>successfully</green>!"
                )
                return true
            }
            return false
        } catch (exception: Exception) {
            logger.error(
                "An error occurred while enabling module ${module.moduleInfo.name}",
                exception.stackTraceToString(),
            )
            return false
        }
    }

    fun disableModule(name: String): Boolean {
        val module = loadedModules.find { module -> module.moduleInfo.name == name } ?: return false
        return disableModule(module)
    }

    fun disableModule(module: LoadedModule): Boolean {
        try {
            if (module.moduleInfo.state == ModuleStates.ENABLED) {
                module.module.onDisable()
                module.moduleInfo.state = ModuleStates.LOADED
                logger.info(
                    "Module <light_purple>${module.moduleInfo.name}</light_purple> has been disabled <green>successfully</green>!"
                )
                return true
            }
            return false
        } catch (exception: Exception) {
            logger.error(
                "An error occurred while disabling module ${module.moduleInfo.name}",
                exception.stackTraceToString(),
            )
            return false
        }
    }

    fun getModule(name: String): LoadedModule? {
        return loadedModules.find { modul -> modul.moduleInfo.name == name }
    }

    fun restartModule(name: String): Boolean {
        val module = loadedModules.find { modul -> modul.moduleInfo.name == name } ?: return false
        return restartModule(module)
    }

    fun restartModule(module: LoadedModule): Boolean {
        val disable = disableModule(module)
        if (!disable) {
            return false
        }
        return enableModule(module)
    }

    fun getAllModules(): List<LoadedModule> {
        return loadedModules
    }

    suspend fun loadAllModules() {
        moduleFolder.toFile().listFiles()?.forEach {
            if (it.extension == "jar") {
                loadModule(it.nameWithoutExtension)
            }
        }
    }

    fun startAllModules() {
        loadedModules.forEach { enableModule(it) }
    }

    fun disableAllModules() {
        loadedModules.forEach { disableModule(it) }
    }

    fun unloadAllModules() {
        loadedModules.forEach { unloadModule(it) }
    }

    fun getAllDownloadableModules(): List<DownloadableModule> {
        val url = URI(modulesJsonURL).toURL()
        val rawJson = url.readText()
        return json.decodeFromString<List<DownloadableModule>>(rawJson)
    }

    fun checkAllLoadedModulesForUpdates() {
        val availableModules = getAllDownloadableModules()
        val installedModules = getAllModules().map { it.moduleInfo }
        availableModules.forEach { availableModule ->
            val installedModule = installedModules.find { it.name == availableModule.name }
            if (installedModule != null && doesModuleHaveAnUpdate(installedModule)) {
                logger.warn(
                    "Module <light_purple>${installedModule.name}</light_purple> has an update! (<yellow>${installedModule.version}</yellow> -> <yellow>${availableModule.version}</yellow>) Install it with 'module update ${availableModule.name}'"
                )
            }
        }
    }

    fun doesModuleHaveAnUpdate(installedModule: ModuleInfo): Boolean {
        val availableModules = getAllDownloadableModules()
        val availableModule = availableModules.find { it.name == installedModule.name }
        if (availableModule == null) {
            return false
        }
        val installedVersion = SemVer.parseOrNull(installedModule.version)
        val availableVersion = SemVer.parseOrNull(availableModule.version)
        if (installedVersion == null || availableVersion == null) {
            return false
        }
        return installedVersion < availableVersion
    }

    suspend fun updateDownloadableModule(module: DownloadableModule): Boolean {
        val installedModule = getAllModules().find { it.moduleInfo.name == module.name }
        if (installedModule != null) {
            val installedVersion = SemVer.parseOrNull(installedModule.moduleInfo.version)
            val newVersion = SemVer.parseOrNull(module.version)
            if (newVersion == null) {
                logger.error("Could not parse version for module ${module.name}")
                return false
            }
            if (installedVersion == null || installedVersion < newVersion) {
                logger.info(
                    "Updating module <light_purple>${module.name}</light_purple> from <yellow>${installedModule.moduleInfo.version}</yellow> to <yellow>${module.version}</yellow>"
                )
                disableModule(installedModule)
                unloadModule(installedModule)
                return installModule(module)
            } else {
                logger.info(
                    "Module <light_purple>${module.name}</light_purple> is already up to date"
                )
                return true
            }
        } else {
            logger.info(
                "Installing module <light_purple>${module.name}</light_purple> (<yellow>v${module.version}</yellow>)"
            )
            return installModule(module)
        }
    }

    fun getDownloadableModule(name: String): DownloadableModule? {
        val availableModules = getAllDownloadableModules()
        return availableModules.find { it.name == name }
    }

    suspend fun forceUpdateModule(module: DownloadableModule): Boolean {
        disableModule(module.name)
        unloadModule(module.name)
        return installModule(module)
    }

    private suspend fun installModule(module: DownloadableModule): Boolean {
        try {
            val url = URI(module.installURL).toURL()
            withContext(Dispatchers.IO) { url.openStream() }
                .use { input ->
                    moduleFolder.resolve("${module.name}.jar").toFile().outputStream().use { output
                        ->
                        input.copyTo(output)
                    }
                }
            loadModule(module.name)
            enableModule(module.name)
            return true
        } catch (exception: Exception) {
            logger.error("Could not install module ${module.name}", exception)
            return false
        }
    }
}
