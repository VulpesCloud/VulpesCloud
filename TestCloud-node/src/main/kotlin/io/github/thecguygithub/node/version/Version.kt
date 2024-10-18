package io.github.thecguygithub.node.version

import io.github.thecguygithub.api.platforms.PlatformTypes
import io.github.thecguygithub.node.platforms.file.PlatformFile
import io.github.thecguygithub.node.platforms.patcher.PlatformPatcher
import io.github.thecguygithub.node.platforms.versions.PlatformVersion

class Version(
    val id: String,
    val type: PlatformTypes,
    val pluginDir: String,
    val pluginData: String,
    var pluginDataPath: String? = null,
    val arguments: List<String>? = null,
    val patchers: List<PlatformPatcher>,
    val versions: List<PlatformVersion>,
    val files: List<PlatformFile>,
    ) {
}