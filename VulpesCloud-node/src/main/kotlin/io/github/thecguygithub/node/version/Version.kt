package io.github.thecguygithub.node.version

import io.github.thecguygithub.api.version.VersionInfo
import io.github.thecguygithub.api.version.VersionType
import io.github.thecguygithub.api.version.Versions
import io.github.thecguygithub.launcher.util.FileSystemUtil
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.service.LocalService
import io.github.thecguygithub.node.util.*
import io.github.thecguygithub.node.version.files.VersionFile
import io.github.thecguygithub.node.version.files.VersionFileStrategy
import io.github.thecguygithub.node.version.patcher.VersionPatcher
import java.nio.file.Files
import java.nio.file.Path

class Version(
    val name: String,
    val type: VersionType,
    val pluginDir: String,
    val arguments: List<String>,
    val patchers: List<VersionPatcher>,
    val files: List<VersionFile>,
    val versions: List<Versions>
) {

    fun prepare(display: VersionInfo, service: LocalService) {
        val version = service.task.version()

        // download only if not exists
        this.download(display, service)

        // copy platform jar and maybe patch files
        DirectoryActions.copyDirectoryContents(Path.of("local/versionCache/${version.name}/${version.versions}"), service.runningDir)

        for (file in files) {
            val strategy = file.strategy
            val target = service.runningDir.resolve(file.file!!)
            val fileType = FileType.define(file.file)

            when (strategy) {
                VersionFileStrategy.COPY_FROM_CLASSPATH_IF_NOT_EXISTS -> {
                    if (!Files.exists(target)) {
                        FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/${version.name}/${file.file}", target.toString())
                    }
                }
                VersionFileStrategy.DIRECT_CREATE -> {
                    Files.deleteIfExists(target)
                    target.parent.toFile().mkdirs()
                    Files.createFile(target)
                }

                null -> Logger().error("Version.kt Strategy is null!")
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
                        Node.taskProvider.tasks()?.any { it.version().name.equals("velocity", ignoreCase = true) }.toString()
                    )

                Files.writeString(target, Files.readString(target) + content)
            }
        }
    }

    fun download(display: VersionInfo, localService: LocalService) {
        val version = versions.firstOrNull { it.version.equals(display.versions, ignoreCase = true) }
            ?: throw NoSuchElementException()

        val platformDir = Path.of("local/versionCache/${display.name}/${display.versions}")

        if (!Files.exists(platformDir)) {
            platformDir.toFile().mkdirs()
        }

        val file = platformDir.resolve("${display.name}-${display.versions}.jar")

        if (!Files.exists(file)) {
            if (version is Versions) {
                Downloader.download(version.link, file)
            }

            patchers.forEach { it.patch(file.toFile(), localService) }
        }
    }
}