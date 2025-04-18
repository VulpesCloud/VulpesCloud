package de.vulpescloud.api.event.events.modules

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.event.Event
import de.vulpescloud.api.module.ModuleInfo

data class ModuleUnloadEvent(val module: ModuleInfo, val node: ClusterNode) : Event
