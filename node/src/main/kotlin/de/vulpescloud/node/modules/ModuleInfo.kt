package de.vulpescloud.node.modules

import kotlinx.serialization.Serializable

@Serializable
data class ModuleInfo(
    val name: String,
    val authors: MutableList<String>,
    val description: String,
    val main: String,
    val version: String,
    var website: String = "<none>",
    val headNodeOnly: Boolean,
    val copyToServices: Boolean,
    val platforms: List<String>,
    var state: ModuleStates = ModuleStates.UNLOADED,
)