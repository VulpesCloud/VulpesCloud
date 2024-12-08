package de.vulpescloud.node.player

import de.vulpescloud.api.player.VulpesPlayer
import de.vulpescloud.api.services.Service
import java.util.*

class VulpesPlayerImpl(
    val name: String,
    val uuid: UUID,
    var currentServer: Service? = null,
    var currentProxy: Service? = null
) : VulpesPlayer {

    override fun uuid(): UUID {
        return this.uuid
    }

    override fun name(): String {
        return this.name
    }

    override fun currentProxy(): Service? {
        return this.currentProxy
    }

    override fun currentServer(): Service? {
        return this.currentServer
    }

    override fun sendTitle(title: String, subtitle: String?, fadeIn: Int, stay: Int, fadeOut: Int) {
        TODO("Not yet implemented")
    }

    override fun sendMessage(message: String) {
        TODO("Not yet implemented")
    }

    override fun sendActionBar(message: String) {
        TODO("Not yet implemented")
    }

    override fun connect(serviceId: String) {
        TODO("Not yet implemented")
    }
}