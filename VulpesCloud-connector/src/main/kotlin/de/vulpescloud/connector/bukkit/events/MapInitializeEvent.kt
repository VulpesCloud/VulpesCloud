package de.vulpescloud.connector.bukkit.events

import de.vulpescloud.connector.bukkit.BukkitConnector
import net.axay.kspigot.event.listen
import org.bukkit.event.server.MapInitializeEvent


object MapInitializeEvent {

    val test = listen<MapInitializeEvent> {
        BukkitConnector.instance.connector.finishStart()
    }

}