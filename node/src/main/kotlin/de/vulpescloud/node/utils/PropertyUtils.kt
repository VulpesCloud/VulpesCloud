package de.vulpescloud.node.utils

object PropertyUtils {

    fun isMoreDBLogging() = System.getProperty("vc.db.logging", "false").toBoolean()

    fun isDBTiming() = System.getProperty("vc.db.timing", "false").toBoolean()

    fun isLoggingPlayerEvents() = System.getProperty("vc.player.events", "false").toBoolean()

    fun isMoreSoftwareLogging() = System.getProperty("vc.software.logging", "false").toBoolean()

    fun isSoftwareTiming() = System.getProperty("vc.software.timing", "false").toBoolean()

    fun isLoggingRedirects() = System.getProperty("vc.redirect.logging", "false").toBoolean()
}
