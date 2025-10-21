package de.vulpescloud.node.modules

import java.net.URLClassLoader

data class LoadedModule(
    val module: VulpesModule,
    val classLoader: URLClassLoader,
    val moduleInfo: ModuleInfo
)