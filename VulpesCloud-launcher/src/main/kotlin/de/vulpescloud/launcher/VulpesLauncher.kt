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

import de.vulpescloud.launcher.config.Config
import de.vulpescloud.launcher.dependency.Dependency
import de.vulpescloud.launcher.dependency.DependencyDownloader
import de.vulpescloud.launcher.updater.*
import de.vulpescloud.launcher.util.ChecksumUtil
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
        val githubURL = "https://github.com/VulpesCloud/VulpesCloud-meta/raw/"
        val config = Config()

        @JvmStatic
        fun main(args: Array<String>) {
            println("Checking for Dependency's to download")

            //

            // config.debug()

            ChecksumUtil.downloadChecksumJson()

            val devMode = System.getProperty("devMode")
            if (devMode != null && devMode.toBoolean() || !config.autoUpdatesEnabled()) {
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
            } else {
                APIUpdater().updateAPI()
                BridgeUpdater().updateBridge()
                ConnectorUpdater().updateConnector()
                NodeUpdater().updateNode()
                WrapperUpdater().updateWrapper()
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
//            val devMode = System.getProperty("devMode")
//            if (devMode == null || !devMode.toBoolean()) {
//                getCloudFilesFromGithub()
//            }

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
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-api.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-api.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-node.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-node.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-wrapper.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-wrapper.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-connector.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-connector.jar")
                )
            )
            FileUpdaterUtil.get(
                URI(githubURL + config.autoUpdatesBranch() + "/vulpescloud-bridge.jar"),
                FileUpdaterUtil.filePathHandler(Path.of("launcher/dependencies/vulpescloud-bridge.jar")
                )
            )
        }
    }
}