package de.vulpescloud.connector.bukkit

import de.vulpescloud.connector.Connector
import de.vulpescloud.connector.bukkit.events.*
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

    override fun load() {
        instance = this
        logger.info("Loading Connector")
        connector.init()
    }

    override fun startup() {
        logger.info("Started Connector")
        // Register Events
        MapInitializeEvent
        PlayerJoinEvent
        connector.registerLocalService()
        connector.finishStart()
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