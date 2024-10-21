package io.github.thecguygithub.node.version

import io.github.thecguygithub.api.version.VersionType
import io.github.thecguygithub.api.version.Versions

class Version(
    val name: String,
    val type: VersionType,
    val pluginDir: String,
    val arguments: List<String>? = null,
//    val patchers: List<PlatformPatcher>,
    val versions: List<Versions>
) {
}