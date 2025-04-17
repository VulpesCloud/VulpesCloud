package de.vulpescloud.api.event.events.modules

import de.vulpescloud.api.cluster.ClusterNode
import de.vulpescloud.api.event.Event
import de.vulpescloud.api.module.ModuleInfo
import kotlinx.serialization.Serializable

@Serializable
data class ModuleUnloadEvent(
    val module: ModuleInfo,
    val node: ClusterNode
) : Event
