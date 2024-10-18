package io.github.thecguygithub.node.platforms

import io.github.thecguygithub.api.Detail
import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.platforms.PlatformTypes
import io.github.thecguygithub.launcher.util.FileSystemUtil
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.platforms.file.PlatformFile
import io.github.thecguygithub.node.platforms.file.PlatformFileStrategy
import io.github.thecguygithub.node.platforms.patcher.PlatformPatcher
import io.github.thecguygithub.node.platforms.versions.PlatformUrlVersion
import io.github.thecguygithub.node.platforms.versions.PlatformVersion
import io.github.thecguygithub.node.service.ClusterLocalServiceImpl
import io.github.thecguygithub.node.util.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate


class Platform(
    val id: String,
    val type: PlatformTypes,
    val pluginDir: String,
    val pluginData: String,
    var pluginDataPath: String? = null,
    val arguments: List<String>? = null,
    val patchers: List<PlatformPatcher>,
    val versions: List<PlatformVersion>,
    val files: List<PlatformFile>,
) : Detail {

    @Throws(Exception::class)
    fun prepare(display: PlatformGroupDisplay, service: ClusterLocalServiceImpl) {
        val platform = service.group().platform()

        // download only if not exists
        this.download(display, service)

        // copy platform jar and maybe patch files
        if (platform != null) {
            DirectoryActions.copyDirectoryContents(Path.of("local/platforms/${platform.platform}/${platform.version}"), service.runningDir)
        }

        for (file in files) {
            val strategy = file.strategy
            val target = service.runningDir.resolve(file.file!!)
            val fileType = FileType.define(file.file)

            when (strategy) {
                PlatformFileStrategy.COPY_FROM_CLASSPATH_IF_NOT_EXISTS -> {
                    if (!Files.exists(target)) {
                        if (platform != null) {
                            FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/${platform.platform}/${file.file}", target.toString())
                        }
                    }
                }
                PlatformFileStrategy.DIRECT_CREATE -> {
                    Files.deleteIfExists(target)
                    target.parent.toFile().mkdirs()
                    Files.createFile(target)
                }

                null -> Logger().error("Something is Wrong in Platform.kt!")
            }

            if (file.replacements!!.isNotEmpty()) {
                val replacer = ConfigManipulator(target.toFile())
                for (replacement in file.replacements) {
                    val content = replacement.value!!
                        .replace("%hostname%", service.hostname())
                        .replace("%port%", service.port().toString())
                        .replace(
                            "%bungeecord_use%",
                            Node.taskProvider?.groups()?.any { it.platform()?.platform.equals("bungeecord", ignoreCase = true) }.toString()
                        )

                    replacer.rewrite(
                        { s -> s.startsWith(replacement.indicator!!) },
                        fileType.replacer.apply(Pair(replacement.indicator, content))
                    )
                }
                replacer.write()
            }

            for (append in file.appends!!) {
                val content = append
                    .replace("%forwarding_secret%", PlatformService.FORWARDING_SECRET)
                    .replace(
                        "%velocity_use%",
                        Node.taskProvider?.groups()?.any { it.platform()?.platform.equals("velocity", ignoreCase = true) }.toString()
                    )

                Files.writeString(target, Files.readString(target) + content)
            }
        }
    }

    @Throws(Exception::class)
    fun download(display: PlatformGroupDisplay, localService: ClusterLocalServiceImpl) {
        val version = versions.firstOrNull { it.version.equals(display.version, ignoreCase = true) }
            ?: throw NoSuchElementException()

        val platformDir = Path.of("local/platforms/${display.platform}/${display.version}")

        if (!Files.exists(platformDir)) {
            platformDir.toFile().mkdirs()
        }

        val file = platformDir.resolve("${display.details()}.jar")

        if (!Files.exists(file)) {
            if (version is PlatformUrlVersion) {
                Downloader.download(version.url, file)
            }

            patchers.forEach { it.patch(file.toFile(), localService) }
        }
    }

    override fun details(): String {
        return "${versions.size} versions&8, &7type &8= &7$type"
    }
}