package de.vulpescloud.api.module

import kotlinx.serialization.Serializable

@Serializable
data class ModuleInfo(
    val name: String,
    val authors: MutableList<String>,
    val description: String,
    val main: String,
    val version: String,
    var website: String = "<none>",
    var state: ModuleStates = ModuleStates.LOADED
)