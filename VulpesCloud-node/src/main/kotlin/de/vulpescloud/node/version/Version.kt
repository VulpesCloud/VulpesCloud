package de.vulpescloud.node.version

import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.api.version.Versions
import de.vulpescloud.launcher.util.FileSystemUtil
import de.vulpescloud.node.Node
import de.vulpescloud.node.service.LocalService
import de.vulpescloud.node.util.*
import de.vulpescloud.node.version.files.VersionFile
import de.vulpescloud.node.version.files.VersionFileStrategy
import de.vulpescloud.node.version.patcher.VersionPatcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class Version(
    val name: String,
    val type: VersionType,
    val pluginDir: String,
    val arguments: List<String>,
    private val patchers: List<VersionPatcher>,
    val files: List<VersionFile>,
    val versions: List<Versions>,
) {

    private val logger: Logger = LoggerFactory.getLogger(Versions::class.java)

    fun prepare(display: VersionInfo, service: LocalService) {
        val version = service.task.version()

        // download only if not exists
        this.download(display, service)

        // copy platform jar and maybe patch files
        DirectoryActions.copyDirectoryContents(
            Path.of("local/versionCache/${version.name}/${version.versions}"),
            service.runningDir
        )

        for (file in files) {
            val strategy = file.strategy
            val target = service.runningDir.resolve(file.file!!)
            val fileType = FileType.define(file.file)

            when (strategy) {
                VersionFileStrategy.COPY_FROM_CLASSPATH_IF_NOT_EXISTS -> {
                    if (!Files.exists(target)) {
                        FileSystemUtil.copyClassPathFile(
                            this::class.java.classLoader,
                            "platforms/${version.name}/${file.file}",
                            target.toString()
                        )
                    }
                }

                VersionFileStrategy.DIRECT_CREATE -> {
                    Files.deleteIfExists(target)
                    target.parent.toFile().mkdirs()
                    Files.createFile(target)
                }

                null -> logger.error("Version.kt Strategy is null!")
            }

            if (file.replacements!!.isNotEmpty()) {
                val replacer = ConfigManipulator(target.toFile())
                for (replacement in file.replacements) {
                    val content = replacement.value!!
                        .replace("%hostname%", service.hostname())
                        .replace("%port%", service.port().toString())

                    replacer.rewrite(
                        { s -> s.startsWith(replacement.indicator!!) },
                        fileType.replacer.apply(Pair(replacement.indicator, content))
                    )
                }
                replacer.write()
            }

            for (append in file.appends!!) {
                val content = append
                    .replace("%forwarding_secret%", Node.versionProvider.FORWARDING_SECRET)
                    .replace(
                        "%velocity_use%",
                        Node.taskProvider.tasks()?.any { it.version().name.equals("velocity", ignoreCase = true) }
                            .toString()
                    )

                Files.writeString(target, Files.readString(target) + content)
            }
        }
    }

    private fun download(display: VersionInfo, localService: LocalService) {
        val version = versions.firstOrNull { it.version.equals(display.versions, ignoreCase = true) }
            ?: throw NoSuchElementException()

        val platformDir = Path.of("local/versionCache/${display.name}/${display.versions}")

        if (!Files.exists(platformDir)) {
            platformDir.toFile().mkdirs()
        }

        val file = platformDir.resolve("${display.name}-${display.versions}.jar")
        if (!Files.exists(file)) {
            logger.error("Downloading and patching file!")
            Downloader.download(version.link, file)

            patchers.forEach { it.patch(file.toFile(), localService) }
        }
    }
}