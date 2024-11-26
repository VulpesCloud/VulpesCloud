package de.vulpescloud.node.module

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path


class ModuleProvider {
    private val moduleFolder = Path.of("modules/")
    private val moduleJsonURL = URI("https://raw.githubusercontent.com/VulpesCloud/VulpesCloud-meta/refs/heads/main/modules.json").toURL()
    private val moduleJsonPath = Path.of("launcher/modules.json")

    init {
        if (!Files.exists(moduleFolder)) {
            Files.createDirectories(moduleFolder)
        }
        Files.writeString(moduleJsonPath, String(moduleJsonURL.openStream().readAllBytes()))
    }

}