package de.vulpescloud.launcher

import de.vulpescloud.launcher.util.FileSystemUtil
import java.io.File
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarFile

class VulpesLauncher {
    companion object {
        val CLASS_LOADER = VulpesClassLoader()
        val DEPENDENCY_DIR: Path = Path.of("launcher/dependencies")

        @JvmStatic
        fun main(args: Array<String>) {
            println(System.getProperty("test"))
        }
    }

    private fun bootFile(): File {
        this.copyBootFiles("api", "node", "wrapper", "connector", "bridge")

        return DEPENDENCY_DIR.resolve("vulpescloud-node.jar").toFile()
    }
    private fun mainClass(): String {
        try {
            JarFile(bootFile()).use { jarFile ->
                val manifest = jarFile.manifest
                if (manifest != null) {
                    val mainAttributes = manifest.mainAttributes
                    return mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
                } else {
                    throw RuntimeException(NullPointerException("No main class detectable!"))
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    private fun copyBootFile(name: String) {
        FileSystemUtil.copyClassPathFile(
            ClassLoader.getSystemClassLoader(),
            "vulpescloud-$name.jar", Path.of("launcher/dependencies/vulpescloud-$name.jar").toString()
        )
    }
    private fun copyBootFiles(vararg names: String) {
        for (name in names) {
            this.copyBootFile(name)
        }
    }
}