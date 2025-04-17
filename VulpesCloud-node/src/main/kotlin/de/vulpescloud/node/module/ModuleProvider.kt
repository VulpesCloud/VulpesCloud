package de.vulpescloud.node.module

import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.node.module.impl.LoadedModule
import java.io.File
import java.nio.file.Path

interface ModuleProvider {

    fun loadModule(file: File): LoadedModule?

    fun loadAllModules()

    fun startAllModules()

    fun startModule(name: String): LoadedModule?

    fun startModule(loadedModule: LoadedModule): LoadedModule?

    fun unloadModule(name: String)

    fun unloadModule(loadedModule: LoadedModule)

    fun reloadModule(name: String)

    fun reloadModule(loadedModule: LoadedModule)

    fun unloadAllModules()

    fun modules(): List<ModuleInfo>

    fun moduleFolder(): Path
}
