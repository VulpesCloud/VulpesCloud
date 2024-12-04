package de.vulpescloud.node.version

import de.vulpescloud.api.version.Environments
import de.vulpescloud.api.version.Version
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.version.patcher.VersionPatcher

class Version(
    val environment: Environments,
    val versionType: VersionType,
    val pluginDir: String,
    val versions: List<Version>,
    val patchers: List<VersionPatcher>
) {

    // todo Implement the stuff when doing services

}