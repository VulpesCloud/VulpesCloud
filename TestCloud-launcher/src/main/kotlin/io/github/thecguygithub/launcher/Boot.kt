package io.github.thecguygithub.launcher

import io.github.thecguygithub.launcher.util.FileSystemUtil
import lombok.SneakyThrows
import java.io.File
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest


class Boot {

    private val DEPENDENCY_DIR: Path = Path.of("local/dependencies")

    @SneakyThrows
    fun bootFile(): File {
        this.copyBootFile("node")

        return DEPENDENCY_DIR.resolve("testcloud-node.jar").toFile()
    }


    fun mainClass(): String {
        try {
            JarFile(bootFile()).use { jarFile ->
                val manifest: Manifest = jarFile.manifest
                run {
                    val mainAttributes = manifest.mainAttributes
                    return mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun copyBootFile(name: String) {
        FileSystemUtil.copyClassPathFile(
            ClassLoader.getSystemClassLoader(),
            "testcloud-$name.jar", Path.of("local/dependencies/testcloud-$name.jar").toString()
        )
    }

    private fun copyBootFiles(vararg names: String) {
        for (name in names) {
            this.copyBootFile(name)
        }
    }


}