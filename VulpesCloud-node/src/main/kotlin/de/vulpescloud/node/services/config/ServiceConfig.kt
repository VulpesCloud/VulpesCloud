package de.vulpescloud.node.services.config


import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.toml.TomlFormat
import com.electronwill.nightconfig.yaml.YamlFormat
import de.vulpescloud.launcher.util.FileSystemUtil
import de.vulpescloud.node.Node
import de.vulpescloud.node.services.LocalServiceImpl
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.*

object ServiceConfig {
    private val logger = LoggerFactory.getLogger(ServiceConfig::class.java)

    fun makeServiceConfigs(service: LocalServiceImpl) {
        when (service.task().version().environment) {
            "VELOCITY" -> {
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
                velocityConfig.set<String>("player-info-forwarding-mode", "modern")

                // save the config
                velocityConfig.save()

                Files.writeString(service.runningDir.resolve("forwarding.secret"), Node.instance.forwardingSecret)
            }
            "PURPUR" -> {
                // Copy the Config
                if (!Files.exists(service.runningDir.resolve("server.properties"))) {
                    FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/server.properties", "${service.runningDir.resolve("server.properties")}")
                }
                val properties = Properties()
                try {
                    properties.load(this::class.java.classLoader.getResourceAsStream("platforms/purpur/server.properties"))

                    properties.setProperty("server-ip", service.hostname())
                    properties.setProperty("server-port", service.port().toString())
                    properties.setProperty("motd", "A VulpesCloud Service!")
                    properties.setProperty("online-mode", false.toString())

                    val out = Files.newOutputStream(service.runningDir.resolve("server.properties"))
                    properties.store(out, "Minecraft server properties - edited by VulpesCloud")

                    properties.clear()

                    properties.setProperty("eula", "true")

                    val outEula = Files.newOutputStream(service.runningDir.resolve("eula.txt"))
                    properties.store(outEula, "Auto Eula by VulpesCloud (https://account.mojang.com/documents/minecraft_eula)")

                    if (!Files.exists(service.runningDir.resolve("config/paper-global.yml"))) {
                        FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/paper-global.yml", "${service.runningDir.resolve("config/paper-global.yml")}")
                    }

                    // Load the Config
                    val globalConf = FileConfig.builder(service.runningDir.resolve("config/paper-global.yml"), YamlFormat.defaultInstance())
                        .sync()
                        .preserveInsertionOrder()
                        .defaultData(this::class.java.classLoader.getResource("platforms/purpur/paper-global.yml"))
                        .build()
                    globalConf.load()
                    globalConf.set<String>("proxies.velocity.secret", Node.instance.forwardingSecret)
                    globalConf.set<Boolean>("proxies.velocity.enabled", true)
                    globalConf.save()
                } catch (e: Exception) {
                    logger.error("Unable to edit server.properties or eula.txt in ${service.runningDir}")
                    logger.error(e.toString())
                }
            }
            "PAPER" -> {
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

                    properties.clear()

                    properties.setProperty("eula", "true")

                    val outEula = Files.newOutputStream(service.runningDir.resolve("eula.txt"))
                    properties.store(outEula, "Auto Eula by VulpesCloud (https://account.mojang.com/documents/minecraft_eula)")

                    if (!Files.exists(service.runningDir.resolve("config/paper-global.yml"))) {
                        FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/paper-global.yml", "${service.runningDir.resolve("config/paper-global.yml")}")
                    }

                    // Load the Config
                    val globalConf = FileConfig.builder(service.runningDir.resolve("config/paper-global.yml"), YamlFormat.defaultInstance())
                        .sync()
                        .preserveInsertionOrder()
                        .defaultData(this::class.java.classLoader.getResource("platforms/purpur/paper-global.yml"))
                        .build()
                    globalConf.load()
                    globalConf.set<String>("proxies.velocity.secret", Node.instance.forwardingSecret)
                    globalConf.set<Boolean>("proxies.velocity.enabled", true)
                    globalConf.save()
                } catch (e: Exception) {
                    logger.error("Unable to edit server.properties or eula.txt in ${service.runningDir}")
                    logger.error(e.toString())
                }
            }
        }
    }

}