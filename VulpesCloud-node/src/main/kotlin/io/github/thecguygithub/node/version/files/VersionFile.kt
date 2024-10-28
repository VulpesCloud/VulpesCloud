package io.github.thecguygithub.node.version.files

class VersionFile(
    val file: String?,
    val strategy: VersionFileStrategy?,
    val replacements: List<VersionFileReplacement>?,
    val appends: List<String>?
)