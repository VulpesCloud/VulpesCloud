package de.vulpescloud.node.modules

import de.vulpescloud.launcher.VulpesLauncher
import de.vulpescloud.node.cluster.ClusterHelper
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.jar.JarFile

class ModuleProvider(private val moduleFolder: Path) {

    private val loadedModules = mutableListOf<LoadedModule>()
    private val classLoaders = mutableMapOf<String, ModuleClassLoader>()

    private val logger = LoggerFactory.getLogger("ModuleProvider")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * METHODS:
     * - loadModule() <>
     * - unloadModule() <>
     * - enableModule() <>
     * - disableModule() <>
     * - getModule() <>
     * - restartModule() <>
     * - getAllModules() <>
     * - getAllDownloadedModules()
     * - downloadModule()
     */
    suspend fun loadModule(name: String): LoadedModule? {
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

                if (moduleInfo.headNodeOnly) {
                    if (!ClusterHelper.getLocalNode().head) {
                        logger.warn(
                            "Refusing to load module ${moduleInfo.name} as it is head node only"
                        )
                        return null
                    }
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
                logger.info("Module ${moduleInfo.name} has been loaded successfully!")
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

                logger.info("Module ${module.moduleInfo.name} has been unloaded successfully!")

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
                logger.info("Module ${module.moduleInfo.name} has been enabled successfully!")
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
                module.moduleInfo.state = ModuleStates.UNLOADED
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
}
