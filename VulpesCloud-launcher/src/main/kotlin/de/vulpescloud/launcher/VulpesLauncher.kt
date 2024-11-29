package de.vulpescloud.launcher

import de.vulpescloud.launcher.dependency.Dependency
import de.vulpescloud.launcher.dependency.DependencyDownloader
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

            println("Checking for Dependency's to download")

            // Setting the Dependency's
            val gsonDependency = Dependency("com.google.code.gson", "gson", "2.11.0")
            val jLineDependency = Dependency("org.jline", "jline", "3.27.1")
            val kotlinSTD = Dependency("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.0")
            val jsonDependency = Dependency("org.json", "json", "20240303")
            val jedisDependency = Dependency("redis.clients", "jedis", "5.2.0")
            val slf4jDependency = Dependency("org.slf4j", "slf4j-api", "2.0.16")
            val logbackCoreDependency = Dependency("ch.qos.logback", "logback-core", "1.5.12")
            val logbackClassicDependency = Dependency("ch.qos.logback", "logback-classic", "1.5.12")
            val hikariCP = Dependency("com.zaxxer", "HikariCP", "6.2.1")
            val mariaDB = Dependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.1")
            val cloud = Dependency("org.incendo", "cloud-core", "2.0.0")
            val cloudExtension = Dependency("org.incendo", "cloud-kotlin-extensions", "2.0.0")
            val cloudAnnotations = Dependency("org.incendo", "cloud-annotations", "2.0.0")
            val cloudKotlinCoroutines = Dependency("org.incendo", "cloud-kotlin-coroutines", "2.0.0")
            val cloudKotlinCoroutinesAnnotations = Dependency("org.incendo", "cloud-kotlin-coroutines-annotations", "2.0.0")
            val jsonConfig = Dependency("com.electronwill.night-config", "json", "3.8.1")
            val yamlConfig = Dependency("com.electronwill.night-config", "yaml", "3.8.1")
            val tomlConfig = Dependency("com.electronwill.night-config", "toml", "3.8.1")
            val coreConfig = Dependency("com.electronwill.night-config", "core", "3.8.1")

            // Downloading the Dependency's
            DependencyDownloader().download(
                gsonDependency,
                jLineDependency,
                kotlinSTD,
                jsonDependency,
                jedisDependency,
                slf4jDependency,
                logbackCoreDependency,
                logbackClassicDependency,
                hikariCP,
                mariaDB,
                cloud,
                cloudExtension,
                cloudAnnotations,
                cloudKotlinCoroutines,
                cloudKotlinCoroutinesAnnotations,
                jsonConfig,
                yamlConfig,
                tomlConfig,
                coreConfig
            )

            this.CLASS_LOADER.addURL(Path.of("launcher/dependencies/vulpescloud-api.jar").toUri().toURL())
            this.CLASS_LOADER.addURL(bootFile().toURI().toURL())

            println("Launching the Node!")
            System.setProperty("startup", System.currentTimeMillis().toString())
            Thread.currentThread().contextClassLoader = this.CLASS_LOADER
            Class.forName(this.mainClass(), true, this.CLASS_LOADER).getMethod("main", Array<String>::class.java).invoke(null, args)
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
}