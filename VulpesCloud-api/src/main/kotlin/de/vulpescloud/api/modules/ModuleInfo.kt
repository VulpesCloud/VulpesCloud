package de.vulpescloud.api.modules

data class ModuleInfo(
    val name: String,
    val author: String,
    val description: String,
    val main: String,
    val version: String,
    var website: String = "<none>",
    var state: ModuleStates = ModuleStates.STOPPED
)
