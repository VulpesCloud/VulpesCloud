package de.vulpescloud.api.event.events.modules

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.modules.VulpesModule

data class ModuleLoadEvent(
    val module: VulpesModule
) : Event {
    override fun name(): String {
        return "ModuleLoadEvent"
    }
}