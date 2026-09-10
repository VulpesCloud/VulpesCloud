/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.vulpesstudios.vulpescloud.launcher

import org.vulpesstudios.vulpescloud.launcher.config.Config
import org.vulpesstudios.vulpescloud.launcher.dependency.DependencyDownloader
import org.vulpesstudios.vulpescloud.launcher.updater.Updater
import org.vulpesstudios.vulpescloud.launcher.util.ChecksumUtil
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.jar.Attributes
import java.util.jar.JarFile

class VulpesLauncher {
    companion object {
        val CLASS_LOADER = VulpesClassLoader()
        val config = Config()
        const val GITHUBURL = "https://github.com/VulpesCloud/VulpesCloud-meta/raw/"

        @JvmStatic
        fun main(args: Array<String>) {
            println("Checking for dependencies to download")

            if (!config.autoUpdatesEnabled()) {
                System.err.println("╭────────────────────────────────────────────────────────╮")
                System.err.println("│                                                        │")
                System.err.println("│                      INFORMATION                       │")
                System.err.println("│                                                        │")
                System.err.println("│  VulpesCloud is currently running in Development Mode! │")
                System.err.println("│                                                        │")
                System.err.println("│  Please make sure that you have all VulpesCloud jars   │")
                System.err.println("│ in the right place, otherwise the cloud will shut down │")
                System.err.println("│                                                        │")
                System.err.println("│   For more information, please ask on our Discord!     │")
                System.err.println("│         VulpesCloud will start in 5 seconds!           │")
                System.err.println("│                                                        │")
                System.err.println("╰────────────────────────────────────────────────────────╯")

                if (System.getProperty("skipWait") == "true") {
                    println("Skipping Wait")
                } else {
                    TimeUnit.SECONDS.sleep(5)
                }

            } else {
                ChecksumUtil.downloadChecksumJson()

                println("Checking for updates...")

                val modulesToUpdate = listOf(
                    "vulpescloud-api",
                    "vulpescloud-bridge",
                    "vulpescloud-connector",
                    "vulpescloud-node",
                    "vulpescloud-wrapper"
                )

                modulesToUpdate.forEach { module ->
                    Updater.updateDependency(module)
                }
            }

            // Download dependencies
            DependencyDownloader().downloadDependencies()

            this.CLASS_LOADER.addURL(Path.of("launcher/dependencies/vulpescloud/vulpescloud-api.jar").toUri().toURL())
            this.CLASS_LOADER.addURL(bootFile().toURI().toURL())

            println("Launching the Node!")
            System.setProperty("startup", System.currentTimeMillis().toString())
            Thread.currentThread().contextClassLoader = this.CLASS_LOADER
            Class.forName(this.mainClass(), true, this.CLASS_LOADER).getMethod("main", Array<String>::class.java)
                .invoke(null, args)
        }

        private fun bootFile(): File {
            return File("launcher/dependencies/vulpescloud/vulpescloud-node.jar")
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
    }
}