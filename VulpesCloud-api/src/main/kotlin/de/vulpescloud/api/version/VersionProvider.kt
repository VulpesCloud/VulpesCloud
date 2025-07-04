package de.vulpescloud.api.version

import java.nio.file.Path

interface VersionProvider {

    fun getVersionByName(name: String): Version?

    fun getVersionsByType(type: VersionType): List<Version>

    fun getAllRegisteredVersions(): List<Version>

    fun registerCustomVersion(version: Version)

    fun prepareVersion(version: SingleVersion, servicePath: Path)
}
