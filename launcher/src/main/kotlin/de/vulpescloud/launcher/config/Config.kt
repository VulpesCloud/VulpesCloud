package de.vulpescloud.launcher.config

import com.electronwill.nightconfig.core.file.CommentedFileConfig
import de.vulpescloud.launcher.util.FileSystemUtil
import java.nio.file.Files
import java.nio.file.Path

class Config {

    private val config = CommentedFileConfig.builder("launcher/launcher-config.toml")
        // .defaultResource("launcher-config.toml")
        .sync()
        .build()

    init {
        if (!Path.of("launcher/launcher-config.toml").toFile().exists()) {
            FileSystemUtil.copyClassPathFile(this::class.java.classLoader, "launcher-config.toml", "launcher/launcher-config.toml")
        }
        config.load()

        try {
            autoUpdatesEnabled()
        } catch (e: NullPointerException) {
            config.set<Boolean>("auto-updates.enabled", true)
            config.save()
        }

        try {
            autoUpdatesBranch()
        } catch (e: NullPointerException) {
            config.set<Boolean>("auto-updates.branch", "stable")
            config.save()
        }
    }

    fun debug() {
        config.set<Boolean>("auto-updates.enabled", true)
        config.set<String>("auto-updates.branch", "development")
        config.save()
    }

    fun autoUpdatesEnabled(): Boolean = config.get("auto-updates.enabled")
    fun autoUpdatesBranch(): String = config.get("auto-updates.branch")


}