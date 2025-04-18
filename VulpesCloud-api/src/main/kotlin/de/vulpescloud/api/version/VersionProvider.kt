package de.vulpescloud.api.version

interface VersionProvider {

    fun getVersionByName(name: String): Version?

    fun getVersionsByType(type: VersionType): List<Version>

}
