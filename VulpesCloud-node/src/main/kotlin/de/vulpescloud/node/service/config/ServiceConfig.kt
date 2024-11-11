package de.vulpescloud.node.service.config

import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.toml.TomlFormat
import de.vulpescloud.node.service.LocalService
import io.github.thecguygithub.launcher.util.FileSystemUtil
import java.nio.file.Files

object ServiceConfig {

    fun makeServiceConfigs(service: LocalService) {
        when (service.task.version().name) {
            "velocity" -> {
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
            }
        }
    }

}