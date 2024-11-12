package de.vulpescloud.connector.bukkit

import de.vulpescloud.connector.Connector
import de.vulpescloud.connector.bukkit.events.*
import net.axay.kspigot.main.KSpigot

class BukkitConnector : KSpigot() {
    val connector = Connector()

    override fun load() {
        instance = this
        connector.init()
    }

    override fun startup() {
        // Register Events
        MapInitializeEvent
        PlayerJoinEvent
    }

    companion object {
        lateinit var instance: BukkitConnector
    }


}