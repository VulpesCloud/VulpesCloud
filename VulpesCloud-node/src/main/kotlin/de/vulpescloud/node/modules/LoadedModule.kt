package de.vulpescloud.node.modules

import de.vulpescloud.api.modules.ModuleInfo
import de.vulpescloud.api.modules.VulpesModule
import java.net.URLClassLoader


data class LoadedModule(
    val module: VulpesModule,
    val classLoad: URLClassLoader,
    val moduleInfo: ModuleInfo
)