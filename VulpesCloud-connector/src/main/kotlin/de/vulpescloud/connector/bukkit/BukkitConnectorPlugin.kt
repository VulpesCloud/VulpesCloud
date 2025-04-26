package de.vulpescloud.connector.bukkit

import de.vulpescloud.connector.common.Connector
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnectorPlugin : JavaPlugin(), Connector {

    override fun onEnable() {
        markOnline()
    }

    override fun onDisable() {
        markStopping()
    }

}
