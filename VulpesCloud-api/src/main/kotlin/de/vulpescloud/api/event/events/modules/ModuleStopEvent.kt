package de.vulpescloud.api.event.events.modules

import de.vulpescloud.api.modules.VulpesModule

data class ModuleStopEvent(
    val module: VulpesModule
)
