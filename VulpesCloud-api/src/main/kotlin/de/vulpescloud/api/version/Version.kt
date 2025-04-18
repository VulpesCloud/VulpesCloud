package de.vulpescloud.api.version

data class Version(
    val name: String,
    val version: String,
    val type: VersionType,
    val downloadURL: String
)
