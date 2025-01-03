package de.vulpescloud.api.version


class VersionInfo(
    val environment: String,
    val versionType: String,
    val version: String
) {
    override fun toString(): String = "$environment:$versionType:$version"
}