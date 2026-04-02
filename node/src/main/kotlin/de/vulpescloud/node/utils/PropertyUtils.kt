package de.vulpescloud.node.utils

object PropertyUtils {

    fun isMoreDBLogging() = System.getProperty("vc.db.logging", "false").toBoolean()

    fun isDBTiming() = System.getProperty("vc.db.timing", "false").toBoolean()
}
