package de.vulpescloud.api.player

import de.vulpescloud.api.Named
import de.vulpescloud.api.services.Service
import java.util.*

interface VulpesPlayer : Named {

    fun uuid(): UUID

    fun currentProxy(): Service

    fun currentServer(): Service

    fun sendTitle(title: String, subtitle: String?, fadeIn: Int, stay: Int, fadeOut: Int)

    fun sendMessage(message: String)

    fun sendActionBar(message: String)

    fun connect(serviceId: String)

    fun sendTitle(title: String, subtitle: String?) {
        //default minecraft values
        this.sendTitle(title, subtitle, 10, 70, 20)
    }

    fun connect(service: Service) {
        this.connect(service.name())
    }
}