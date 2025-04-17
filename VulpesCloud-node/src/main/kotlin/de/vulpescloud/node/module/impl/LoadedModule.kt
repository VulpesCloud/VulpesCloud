package de.vulpescloud.node.module.impl

import de.vulpescloud.api.module.ModuleInfo
import de.vulpescloud.api.module.VulpesModule
import java.net.URLClassLoader

data class LoadedModule(
    val module: VulpesModule,
    val classLoader: URLClassLoader,
    val moduleInfo: ModuleInfo
)