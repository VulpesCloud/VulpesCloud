package de.vulpescloud.connector.bukkit

import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector : JavaPlugin() {

    override fun onEnable() {
        logger.info("Successfully loaded the Bukkit Connector!")
    }

}
