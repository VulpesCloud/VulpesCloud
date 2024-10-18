package io.github.thecguygithub.api.platforms

enum class PlatformTypes(
    val defaultRuntimePort: Int,
    val defaultTemplateSpace: String,
    val shutdownTypeCommand: String?
) {

    PROXY(25565, "every_proxy", "end"),
    SERVER(20000, "every_server", "stop");

}