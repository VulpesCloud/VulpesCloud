package de.vulpescloud.connector.bukkit

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.services.builder.ServiceRegistrationMessageBuilder
import de.vulpescloud.bridge.service.ServiceProvider
import de.vulpescloud.connector.Connector
import de.vulpescloud.wrapper.Wrapper
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

    companion object {
        lateinit var instance: BukkitConnector
    }

    init {
        instance = this
    }

    override fun load() {
        logger.info("Loading Connector")
        connector.init()
    }

    override fun startup() {
        BukkitRedisListener()
        // Register Events
        // PlayerJoinEvent
        registerLocalService()
        connector.finishStart()

        logger.info("Started Connector")
    }

    override fun shutdown() {
        logger.info("Goodbye!")
        unregisterLocalService()
        connector.shutdownLocal()
    }

    private fun registerLocalService() {
        Wrapper.instance.getRC()?.sendMessage(
            ServiceRegistrationMessageBuilder.serviceRegisterBuilder()
                .setService(ServiceProvider.findServiceById(Wrapper.instance.service.id)!!)
                .build(),
            RedisChannelNames.VULPESCLOUD_SERVICE_REGISTER.name
        )
    }

    private fun unregisterLocalService() {
        Wrapper.instance.getRC()?.sendMessage(
            ServiceRegistrationMessageBuilder.serviceUnregisterBuilder()
                .setService(ServiceProvider.findServiceById(Wrapper.instance.service.id)!!)
                .build(),
            RedisChannelNames.VULPESCLOUD_SERVICE_REGISTER.name
        )
    }

}