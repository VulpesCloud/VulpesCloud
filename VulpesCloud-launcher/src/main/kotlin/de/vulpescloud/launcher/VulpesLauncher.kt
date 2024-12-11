/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.launcher

import de.vulpescloud.launcher.dependency.Dependency
import de.vulpescloud.launcher.dependency.DependencyDownloader
import de.vulpescloud.launcher.util.FileUpdaterUtil
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.jar.Attributes
import java.util.jar.JarFile
import kotlin.system.exitProcess

class VulpesLauncher {
    companion object {
        val CLASS_LOADER = VulpesClassLoader()
        val DEPENDENCY_DIR: Path = Path.of("launcher/dependencies")
        private val githubURL = "https://github.com/VulpesCloud/VulpesCloud-meta/raw/main/"

        @JvmStatic
        fun main(args: Array<String>) {
            println("Checking for Dependency's to download")

            // Setting the Dependency's
            val gsonDependency = Dependency("com.google.code.gson", "gson", "2.11.0")
            val jLineDependency = Dependency("org.jline", "jline", "3.28.0")
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
            val exposedCore = Dependency("org.jetbrains.exposed", "exposed-core", "0.57.0")
            val exposedJDBC = Dependency("org.jetbrains.exposed", "exposed-jdbc", "0.57.0")
            val snakeYAML = Dependency("org.yaml", "snakeyaml", "2.3")

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
                coreConfig,
                exposedCore,
                exposedJDBC,
                snakeYAML,
            )

            val devMode = System.getProperty("devMode")
            if (devMode != null && devMode.toBoolean()) {
                System.err.println("╭────────────────────────────────────────────────────────╮")
                System.err.println("│                                                        │")
                System.err.println("│                      INFORMATION                       │")
                System.err.println("│                                                        │")
                System.err.println("│  VulpesCloud is currently running in Development Mode! │")
                System.err.println("│                                                        │")
                System.err.println("│  Please make sure that you have all VulpesCloud jars   │")
                System.err.println("│ in the right place, otherwise the Cloud will shut down │")
                System.err.println("│                                                        │")
                System.err.println("│   For more Information, please ask on our Discord!     │")
                System.err.println("│         VulpesCloud will start in 5 seconds!           │")
                System.err.println("│                                                        │")
                System.err.println("╰────────────────────────────────────────────────────────╯")

                if (System.getProperty("skipWait") == "true") {
                    println("Skipping Wait")
                } else {
                    TimeUnit.SECONDS.sleep(5)
                }

                val apiFile = Path.of("launcher/dependencies/vulpescloud-api.jar").toFile()
                val nodeFile = Path.of("launcher/dependencies/vulpescloud-node.jar").toFile()
                val wrapperFile = Path.of("launcher/dependencies/vulpescloud-wrapper.jar").toFile()
                val connectorFile = Path.of("launcher/dependencies/vulpescloud-connector.jar").toFile()
                val bridgeFile = Path.of("launcher/dependencies/vulpescloud-bridge.jar").toFile()

                if (!apiFile.exists()) {
                    System.err.println("vulpescloud-api.jar not found in dependencies folder! Put the file there, or disable Development mode!")
                    exitProcess(-1)
                }
                if (!nodeFile.exists()) {
                    System.err.println("vulpescloud-node.jar not found in dependencies folder! Put the file there, or disable Development mode!")
                    exitProcess(-1)
                }
                if (!wrapperFile.exists()) {
                    System.err.println("vulpescloud-wrapper.jar not found in dependencies folder! Put the file there, or disable Development mode!")
                    exitProcess(-1)
                }
                if (!connectorFile.exists()) {
                    System.err.println("vulpescloud-connector.jar not found in dependencies folder! Put the file there, or disable Development mode!")
                    exitProcess(-1)
                }
                if (!bridgeFile.exists()) {
                    System.err.println("vulpescloud-bridge.jar not found in dependencies folder! Put the file there, or disable Development mode!")
                    exitProcess(-1)
                }
            }

            this.CLASS_LOADER.addURL(Path.of("launcher/dependencies/vulpescloud-api.jar").toUri().toURL())
            this.CLASS_LOADER.addURL(bootFile().toURI().toURL())

            println("Launching the Node!")
            System.setProperty("startup", System.currentTimeMillis().toString())
            Thread.currentThread().contextClassLoader = this.CLASS_LOADER
            Class.forName(this.mainClass(), true, this.CLASS_LOADER).getMethod("main", Array<String>::class.java)
                .invoke(null, args)
        }

        private fun bootFile(): File {
            val devMode = System.getProperty("devMode")
            if (devMode == null || !devMode.toBoolean()) {
                getCloudFilesFromGithub()
            }

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

        private fun getCloudFilesFromGithub() {
            FileUpdaterUtil.get(
                URI(githubURL + "vulpescloud-api.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-api.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + "vulpescloud-node.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-node.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + "vulpescloud-wrapper.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-wrapper.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + "vulpescloud-connector.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-connector.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + "vulpescloud-bridge.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-bridge.jar")
                )
            )
        }
    }
}