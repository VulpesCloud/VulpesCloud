package de.vulpescloud.node.version

import de.vulpescloud.api.version.Environments
import de.vulpescloud.api.version.Version
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.services.LocalServiceImpl
import de.vulpescloud.node.util.DirectoryActions
import de.vulpescloud.node.version.patcher.VersionPatcher
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class Version(
    val environment: Environments,
    val versionType: VersionType,
    val pluginDir: String,
    val versions: List<Version>,
    val patchers: List<VersionPatcher>
) {

    private val logger = LoggerFactory.getLogger(Version::class.java)

    fun prepare(display: VersionInfo, service: LocalServiceImpl) {
        val version = service.task().version()

        // download only if not exists
        this.download(display, service)

        // copy platform jar and maybe patch files
        DirectoryActions.copyDirectoryContents(
            Path.of("local/versionCache/${version.environment}/${version.version}"),
            service.runningDir
        )
    }

    private fun download(display: VersionInfo, localService: LocalServiceImpl) {
        val version = versions.firstOrNull { it.version.equals(display.version, ignoreCase = true) }
            ?: throw NoSuchElementException()

        val platformDir = Path.of("local/versionCache/${display.environment}/${display.version}")

        if (!Files.exists(platformDir)) {
            platformDir.toFile().mkdirs()
        }

        val file = platformDir.resolve("${display.environment}-${display.version}.jar")
        if (!Files.exists(file)) {
            logger.error("Downloading and patching file!")
            Files.copy(
                URI.create(version.link).toURL().openConnection().getInputStream(),
                file
            )

            patchers.forEach { it.patch(file.toFile(), localService) }
        }
    }

}