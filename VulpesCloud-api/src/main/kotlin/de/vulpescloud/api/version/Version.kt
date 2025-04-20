package de.vulpescloud.api.version

data class Version(
    val name: String,
    val type: VersionType,
    val pluginsDir: String,
    val versions: List<SingleVersion>
)

data class SingleVersion(
    val name: String,
    val version: String,
    val downloadURL: String
)
