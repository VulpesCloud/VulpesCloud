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

package de.vulpescloud.node.service.config

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.toml.TomlFormat
import de.vulpescloud.launcher.util.FileSystemUtil
import de.vulpescloud.node.service.LocalService
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.*

object ServiceConfig {

    private val logger = LoggerFactory.getLogger(ServiceConfig::class.java)

    fun makeServiceConfigs(service: LocalService) {
        when (service.task.version().name) {
            "Velocity" -> {
                // Copy the Config
                if (!Files.exists(service.runningDir.resolve("velocity.toml"))) {
                    FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/Velocity/velocity.toml", "${service.runningDir.resolve("velocity.toml")}")
                }

                // Load the Config
                val velocityConfig = FileConfig.builder(service.runningDir.resolve("velocity.toml"), TomlFormat.instance())
                    .sync()
                    .preserveInsertionOrder()
                    .defaultData(this::class.java.classLoader.getResource("platforms/Velocity/velocity.toml"))
                    .build()
                velocityConfig.load()

                // Set the Stuff in the Config
                velocityConfig.set<String>("bind", service.hostname() + ":" + service.port())

                // save the config
                velocityConfig.save()

                Files.writeString(service.runningDir.resolve("forwarding.secret"), "lhg8u6asid7zrg")
            }
            "Purpur" -> {
                // Copy the Config
                if (!Files.exists(service.runningDir.resolve("server.properties"))) {
                    FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/server.properties", "${service.runningDir.resolve("server.properties")}")
                }
                val properties = Properties()
                try {
                    logger.info("Loading Properties")
                    properties.load(this::class.java.classLoader.getResourceAsStream("platforms/purpur/server.properties"))

                    logger.info("Setting server stugg")
                    properties.setProperty("server-ip", service.hostname())
                    properties.setProperty("server-port", service.port().toString())
                    properties.setProperty("motd", "A VulpesCloud Service!")
                    properties.setProperty("online-mode", false.toString())

                    logger.info("Writing out the File to {}", service.runningDir.resolve("server.properties"))
                    val out = Files.newOutputStream(service.runningDir.resolve("server.properties"))
                    properties.store(out, "Minecraft server properties - edited by VulpesCloud")

                    properties.clear();

                    properties.setProperty("eula", "true")

                    val outEula = Files.newOutputStream(service.runningDir.resolve("eula.txt"))
                    properties.store(outEula, "Auto Eula by VulpesCloud (https://account.mojang.com/documents/minecraft_eula)")
                } catch (e: Exception) {
                    logger.error("Unable to edit server.properties or eula.txt in ${service.runningDir}")
                    logger.error(e.toString())
                }
            }
            "Paper" -> {
                // Copy the Config
                if (!Files.exists(service.runningDir.resolve("server.properties"))) {
                    FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/server.properties", "${service.runningDir.resolve("server.properties")}")
                }

                val properties = Properties()
                val logger = LoggerFactory.getLogger(ServiceConfig::class.java)
                try {
                    logger.info("Loading Properties")
                    properties.load(this::class.java.classLoader.getResourceAsStream("platforms/purpur/server.properties"))

                    logger.info("Setting server stugg")
                    properties.setProperty("server-ip", service.hostname())
                    properties.setProperty("server-port", service.port().toString())
                    properties.setProperty("motd", "A VulpesCloud Service!")
                    properties.setProperty("online-mode", false.toString())

                    logger.info("Writing out the File to {}", service.runningDir.resolve("server.properties"))
                    val out = Files.newOutputStream(service.runningDir.resolve("server.properties"))
                    properties.store(out, "Minecraft server properties - edited by VulpesCloud")

                    properties.clear();

                    properties.setProperty("eula", "true")

                    val outEula = Files.newOutputStream(service.runningDir.resolve("eula.txt"))
                    properties.store(outEula, "Auto Eula by VulpesCloud (https://account.mojang.com/documents/minecraft_eula)")
                } catch (e: Exception) {
                    logger.error("Unable to edit server.properties or eula.txt in ${service.runningDir}")
                    logger.error(e.toString())
                }
            }
        }
    }
}