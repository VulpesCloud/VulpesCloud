package de.vulpescloud.connector.bukkit

import de.vulpescloud.connector.Connector
import de.vulpescloud.connector.bukkit.events.*
import de.vulpescloud.connector.velocity.VelocityConnector
import de.vulpescloud.connector.velocity.VelocityConnector.Companion
import net.axay.kspigot.extensions.server
import net.axay.kspigot.main.KSpigot
import net.kyori.adventure.text.minimessage.MiniMessage

class BukkitConnector : KSpigot() {
    val connector = Connector()
    private val miniMessages = MiniMessage.miniMessage()
    private val logger = LocalLogger()

    class LocalLogger {
        private val miniMessages = MiniMessage.miniMessage()
        fun info(message: String) {
            server.consoleSender.sendMessage(miniMessages.deserialize("<gray>[</gray><color:#C800FF>VulpesCloud-Connector</color><gray>]</gray>  <white>$message</white>"))
        }

        fun warn(message: String) {
            server.consoleSender.sendMessage(miniMessages.deserialize("<gray>[</gray><color:#C800FF>VulpesCloud-Connector</color><gray>]</gray>  <yellow>$message</yellow>"))
        }

        fun error(message: String) {
            server.consoleSender.sendMessage(miniMessages.deserialize("<gray>[</gray><color:#C800FF>VulpesCloud-Connector</color><gray>]</gray>  <red>$message</red>"))
        }
    }

    init {
        instance = this
    }

    override fun load() {
        logger.info("Loading Connector")
        connector.init()
    }

    override fun startup() {
        BukkitRedisSubscribe()
        // Register Events
        MapInitializeEvent
        PlayerJoinEvent
        connector.registerLocalService()
        connector.finishStart()

        logger.info("Started Connector")
    }

    override fun shutdown() {
        logger.info("Goodbye!")
        connector.unregisterLocalService()
        connector.shutdownLocal()
    }

    companion object {
        lateinit var instance: BukkitConnector
    }


}