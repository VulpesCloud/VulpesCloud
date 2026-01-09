package de.vulpescloud.node.services

import de.vulpescloud.api.services.Service

interface AbstractService {
    val service: Service

    fun start()

    fun stop()

    fun delete()

    fun command(command: String)

    fun restart()
}
