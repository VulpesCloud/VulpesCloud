package de.vulpescloud.api.redis

object SenderUtils {

    fun getSenderName(): String {
        return System.getProperty("VULPESCLOUD.DEFAULT.SENDER", "Unknown")
    }

}
