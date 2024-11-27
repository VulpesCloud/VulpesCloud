package de.vulpescloud.node.module

import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.api.module.VulpesModule
import java.net.URLClassLoader

data class LoadedModule(
    val module: VulpesModule,
    val classLoad: URLClassLoader,
    val moduleInfo: ModuleInfo
)
