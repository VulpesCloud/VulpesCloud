/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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