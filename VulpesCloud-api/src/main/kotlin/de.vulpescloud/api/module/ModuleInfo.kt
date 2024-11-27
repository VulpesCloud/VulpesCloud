package de.vulpescloud.api.module

data class ModuleInfo(
    val name: String,
    val author: String,
    val description: String,
    val main: String,
    val version: String,
    var website: String = "<none>",
    var states: ModuleStates = ModuleStates.STOPPED
)
