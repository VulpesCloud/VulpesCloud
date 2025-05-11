package de.vulpescloud.api.module

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
    var state: ModuleStates = ModuleStates.LOADED,
)
