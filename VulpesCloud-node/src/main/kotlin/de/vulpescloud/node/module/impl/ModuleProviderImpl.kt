package de.vulpescloud.node.module.impl

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.events.modules.ModuleLoadEvent
import de.vulpescloud.api.event.events.modules.ModuleStartEvent
import de.vulpescloud.api.event.events.modules.ModuleUnloadEvent
import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.api.module.ModuleStates
import de.vulpescloud.api.module.VulpesModule
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.launcher.VulpesLauncher
import de.vulpescloud.node.event.EventManagerImpl
import de.vulpescloud.node.module.ModuleProvider
import de.vulpescloud.node.utils.JsonUtils.getModuleInfo
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStreamReader
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.Path

class ModuleProviderImpl(eventManager: EventManager, private val clusterProvider: ClusterProvider) :
    ModuleProvider {

    private val modules = mutableMapOf<String, LoadedModule>()
    private val loadedFiles = mutableMapOf<String, File>()
    private val classLoaders = mutableMapOf<String, URLClassLoader>()

    private val moduleFolder = Path("modules")

    private val eventManager = eventManager as EventManagerImpl

    private val logger = LoggerFactory.getLogger(ModuleProviderImpl::class.java)

    override fun loadModule(file: File): LoadedModule? {
        try {
            logger.debug("Trying to load module named &m${file.name}")

            val jarFile = JarFile(file)
            val moduleJson = jarFile.getJarEntry("module.json")
            if (moduleJson == null) {
                logger.warn("Module named &m${file.name} &7has no module.json")
                return null
            }
            val reader = InputStreamReader(jarFile.getInputStream(moduleJson))
            val moduleInfo = getModuleInfo(JSONObject(reader.readText()))

            val classLoader =
                URLClassLoader(arrayOf(file.toURI().toURL()), VulpesLauncher.CLASS_LOADER)
            val clazz = classLoader.loadClass(moduleInfo.main)
            if (!VulpesModule::class.java.isAssignableFrom(clazz)) {
                logger.warn("Class ${moduleInfo.main} does not implement VulpesModule")
                return null
            }
            val vulpesModule = clazz.getDeclaredConstructor().newInstance() as VulpesModule
            val loadedModule = LoadedModule(vulpesModule, classLoader, moduleInfo)

            if (modules.containsKey(loadedModule.moduleInfo.name)) {
                return modules[loadedModule.moduleInfo.name]
            }

            modules[loadedModule.moduleInfo.name] = loadedModule
            classLoaders[loadedModule.moduleInfo.name] = classLoader
            loadedFiles[loadedModule.moduleInfo.name] = file

            eventManager.callGlobal(
                ModuleLoadEvent(loadedModule.moduleInfo, clusterProvider.localNode()),
                RedisChannels.VULPESCLOUD_EVENT_MODULE_ModuleLoadEvent,
            )

            return loadedModule
        } catch (e: Exception) {
            logger.error(
                "Exception whilst trying to load Module ${file.name}. Exception: ${e.message}"
            )
            return null
        }
    }

    override fun startModule(loadedModule: LoadedModule): LoadedModule? {
        try {
            if (loadedModule.moduleInfo.state == ModuleStates.STARTED) return loadedModule

            logger.debug("Trying to start Module ${loadedModule.moduleInfo.name}")

            loadedModule.module.onEnable()

            loadedModule.moduleInfo.state = ModuleStates.STARTED

            modules.replace(loadedModule.moduleInfo.name, loadedModule)

            eventManager.callGlobal(
                ModuleStartEvent(loadedModule.moduleInfo, clusterProvider.localNode()),
                RedisChannels.VULPESCLOUD_EVENT_MODULE_ModuleStartEvent,
            )
            return loadedModule
        } catch (e: Exception) {
            logger.warn(
                "Exception whilst trying to start Module ${loadedModule.moduleInfo.name}. Exception: ${e.message}"
            )
            return null
        }
    }

    override fun startModule(name: String): LoadedModule? {
        val module = modules[name] ?: return null
        return startModule(module)
    }

    override fun loadAllModules() {
        moduleFolder.toFile().mkdirs()

        val list =
            moduleFolder.toFile().listFiles { file -> file.extension == "jar" }?.toList()
                ?: emptyList()

        for (file in list) {
            loadModule(file)
        }
    }

    override fun startAllModules() {
        modules
            .filter { it.value.moduleInfo.state == ModuleStates.LOADED }
            .forEach { startModule(it.value) }
    }

    override fun unloadModule(loadedModule: LoadedModule) {
        try {
            logger.debug("Trying to unload Module ${loadedModule.moduleInfo.name}")
            if (loadedModule.moduleInfo.state == ModuleStates.STARTED) {
                loadedModule.module.onDisable()
            }

            modules.remove(loadedModule.moduleInfo.name)
            classLoaders.remove(loadedModule.moduleInfo.name)
            loadedFiles.remove(loadedModule.moduleInfo.name)

            loadedModule.classLoader.close()

            eventManager.callGlobal(
                ModuleUnloadEvent(loadedModule.moduleInfo, clusterProvider.localNode()),
                RedisChannels.VULPESCLOUD_EVENT_MODULE_ModuleUnloadEvent,
            )
        } catch (e: Exception) {
            logger.warn(
                "Exception whilst trying to disable module ${loadedModule.moduleInfo.name}. Exception: ${e.message}"
            )
        }
    }

    override fun unloadModule(name: String) {
        val module = modules[name] ?: return
        return unloadModule(module)
    }

    override fun unloadAllModules() {
        modules.forEach { unloadModule(it.value) }
    }

    override fun reloadModule(loadedModule: LoadedModule) {
        val file = loadedFiles[loadedModule.moduleInfo.name] ?: return
        unloadModule(loadedModule)
        loadModule(file)
    }

    override fun reloadModule(name: String) {
        val module = modules[name] ?: return
        return reloadModule(module)
    }

    override fun modules(): List<ModuleInfo> {
        return modules.values.map { it.moduleInfo }
    }

    override fun moduleFolder(): Path {
        return moduleFolder
    }
}
