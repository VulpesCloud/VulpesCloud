/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.module

import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.api.module.ModuleStates
import de.vulpescloud.api.module.VulpesModule
import de.vulpescloud.launcher.VulpesLauncher
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile


class ModuleProvider {
    private val moduleFolder = Path.of("modules/")
    private val moduleJsonURL = URI("https://raw.githubusercontent.com/VulpesCloud/VulpesCloud-meta/refs/heads/main/modules.json").toURL()
    private val moduleJsonPath = Path.of("launcher/modules.json")
    private val logger = LoggerFactory.getLogger(ModuleProvider::class.java)
    private val modules: MutableList<ModuleInfo> = mutableListOf()
    private val loadedModules: MutableList<LoadedModule> = mutableListOf()

    init {
        if (!Files.exists(moduleFolder)) {
            Files.createDirectories(moduleFolder)
        }
        Files.writeString(moduleJsonPath, String(moduleJsonURL.openStream().readAllBytes()))
    }

    fun loadAllModules() {
        val files = moduleFolder.toFile().listFiles { _, name -> name.endsWith(".jar") }
        if (files == null) {
            return
        }
        val fileList = files.toList()

        for (file in fileList) {
            val jarFile: JarFile = try {
                JarFile(file)
            } catch (ignore: Exception) {
                continue
            }
            val moduleJson = jarFile.getJarEntry("module.json")
            if (moduleJson == null) {
                logger.error("Jar File &b{} does not contain a module.json and is in the modules folder!", file.name)
                continue
            }
            val reader = InputStreamReader(jarFile.getInputStream(moduleJson))
            val json = JSONObject(reader.readText())
            val moduleInfo = getModuleInfoFromJson(json)
            if (moduleInfo == null) {
                logger.error("Module {} contains an invalid module.json", file.name)
                continue
            }
            logger.info("Loading Module &m{}", moduleInfo.name)
            try {
                val vulpesModule = this.loadModule(file, moduleInfo.main)
                val classLoader = vulpesModule.javaClass.classLoader as URLClassLoader
                val loadedModule = LoadedModule(vulpesModule, classLoader, moduleInfo)
                loadedModule.moduleInfo.states = ModuleStates.LOADED
                this.modules.add(moduleInfo)
                this.loadedModules.add(loadedModule)

            } catch (e: Exception) {
                logger.error("Failed to load Module {}", moduleInfo.name)
                e.printStackTrace()
            }
        }

        if (this.loadedModules.isNotEmpty()) {
            this.loadedModules.forEach {
                logger.info("Starting Module &m{}", it.moduleInfo.name)
                it.module.enable()
                it.moduleInfo.states = ModuleStates.STARTED
            }
        }
    }

    fun unloadAllModules() {
        this.loadedModules.forEach {
            logger.info("Unloading Module &m{}", it.moduleInfo.name)
            it.module.disable()
            it.moduleInfo.states = ModuleStates.STOPPED
        }
    }

    private fun loadModule(file: File, main: String): VulpesModule {
        val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()), VulpesLauncher.CLASS_LOADER)
        val clazz = classLoader.loadClass(main)
        if (!VulpesModule::class.java.isAssignableFrom(clazz)) {
            throw IllegalArgumentException("Class $main does not implement VulpesModule")
        }
        return clazz.getDeclaredConstructor().newInstance() as VulpesModule
    }
    
    fun loadedModules(): List<LoadedModule> {
        return loadedModules
    }

    private fun getModuleInfoFromJson(json: JSONObject): ModuleInfo? {
        try {
            return if (json.has("website")) {
                ModuleInfo(
                    json.getString("name"),
                    json.getString("author"),
                    json.getString("description"),
                    json.getString("main"),
                    json.getString("version"),
                    json.getString("website")
                )
            } else {
                ModuleInfo(
                    json.getString("name"),
                    json.getString("author"),
                    json.getString("description"),
                    json.getString("main"),
                    json.getString("version")
                )
            }
        } catch (e: Exception) {
            return null
        }
    }

}