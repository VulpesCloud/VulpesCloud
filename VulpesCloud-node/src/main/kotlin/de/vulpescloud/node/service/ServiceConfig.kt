package de.vulpescloud.node.service

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.toml.TomlFormat
import com.electronwill.nightconfig.yaml.YamlFormat
import de.vulpescloud.launcher.util.FileSystemUtil
import de.vulpescloud.node.Node
import de.vulpescloud.node.config.NodeConfig
import kotlinx.coroutines.Runnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.*

object ServiceConfig : KoinComponent {

    private val logger = LoggerFactory.getLogger(ServiceConfig::class.java)
    private val nodeConfig: NodeConfig by inject()

    fun updateConfigs(service: LocalService) {
        when (service.task.version.name.lowercase()) {
            "paper" -> {
                // Copy the Config
                if (!Files.exists(service.path().resolve("server.properties"))) {
                    FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/server.properties", "${service.path().resolve("server.properties")}")
                }
                val properties = Properties()
                try {
                    properties.load(service.path().resolve("server.properties").toFile().inputStream())

                    properties.setProperty("server-ip", nodeConfig.hostname())
                    properties.setProperty("server-port", service.port.toString())
                    properties.setProperty("motd", "A VulpesCloud Service!")
                    properties.setProperty("online-mode", false.toString())
                    properties.setProperty("max-players", service.maxPlayers.toString())

                    val out = Files.newOutputStream(service.path().resolve("server.properties"))
                    properties.store(out, "Minecraft server properties - edited by VulpesCloud")

                    properties.clear()

                    properties.setProperty("eula", "true")

                    val outEula = Files.newOutputStream(service.path().resolve("eula.txt"))
                    properties.store(outEula, "Auto Eula by VulpesCloud (https://account.mojang.com/documents/minecraft_eula)")

                    if (!Files.exists(service.path().resolve("config/paper-global.yml"))) {
                        FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/paper-global.yml", "${service.path().resolve("config/paper-global.yml")}")
                    }

                    // Load the Config
                    val globalConf = FileConfig.builder(service.path().resolve("config/paper-global.yml"), YamlFormat.defaultInstance())
                        .sync()
                        .preserveInsertionOrder()
                        .defaultData(this::class.java.classLoader.getResource("platforms/purpur/paper-global.yml"))
                        .build()
                    globalConf.load()
                    globalConf.set<String>("proxies.velocity.secret", Node.getForwardingSecret())
                    globalConf.set<Boolean>("proxies.velocity.enabled", true)
                    globalConf.save()
                } catch (e: Exception) {
                    logger.error("Unable to edit server.properties or eula.txt in ${service.path()}")
                    logger.error(e.toString())
                }
            }
            "purpur" -> {
                // Copy the Config
                if (!Files.exists(service.path().resolve("server.properties"))) {
                    FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/server.properties", "${service.path().resolve("server.properties")}")
                }
                val properties = Properties()
                try {
                    properties.load(service.path().resolve("server.properties").toFile().inputStream())

                    properties.setProperty("server-ip", nodeConfig.hostname())
                    properties.setProperty("server-port", service.port.toString())
                    properties.setProperty("motd", "A VulpesCloud Service!")
                    properties.setProperty("online-mode", false.toString())
                    properties.setProperty("max-players", service.maxPlayers.toString())

                    val out = Files.newOutputStream(service.path().resolve("server.properties"))
                    properties.store(out, "Minecraft server properties - edited by VulpesCloud")

                    properties.clear()

                    properties.setProperty("eula", "true")

                    val outEula = Files.newOutputStream(service.path().resolve("eula.txt"))
                    properties.store(outEula, "Auto Eula by VulpesCloud (https://account.mojang.com/documents/minecraft_eula)")

                    if (!Files.exists(service.path().resolve("config/paper-global.yml"))) {
                        FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/purpur/paper-global.yml", "${service.path().resolve("config/paper-global.yml")}")
                    }

                    // Load the Config
                    val globalConf = FileConfig.builder(service.path().resolve("config/paper-global.yml"), YamlFormat.defaultInstance())
                        .sync()
                        .preserveInsertionOrder()
                        .defaultData(this::class.java.classLoader.getResource("platforms/purpur/paper-global.yml"))
                        .build()
                    globalConf.load()
                    globalConf.set<String>("proxies.velocity.secret", Node.getForwardingSecret())
                    globalConf.set<Boolean>("proxies.velocity.enabled", true)
                    globalConf.save()
                } catch (e: Exception) {
                    logger.error("Unable to edit server.properties or eula.txt in ${service.path()}")
                    logger.error(e.toString())
                }
            }
            "velocity" -> {
                // Copy the Config
                if (!Files.exists(service.path().resolve("velocity.toml"))) {
                    FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "platforms/Velocity/velocity.toml", "${service.path().resolve("velocity.toml")}")
                }

                // Load the Config
                val velocityConfig = FileConfig.builder(service.path().resolve("velocity.toml"), TomlFormat.instance())
                    .sync()
                    .preserveInsertionOrder()
                    .defaultData(this::class.java.classLoader.getResource("platforms/Velocity/velocity.toml"))
                    .build()
                velocityConfig.load()

                // Set the Stuff in the Config
                velocityConfig.set<String>("bind", nodeConfig.hostname() + ":" + service.port)
                velocityConfig.set<String>("player-info-forwarding-mode", "modern")

                velocityConfig.set<Int>("show-max-players", service.maxPlayers)

                // save the config
                velocityConfig.save()

                Files.writeString(service.path().resolve("forwarding.secret"), Node.getForwardingSecret())
            }
            "minestom" -> {
                Files.writeString(service.path().resolve("forwarding.secret"), Node.getForwardingSecret())
            }
        }
    }
}
