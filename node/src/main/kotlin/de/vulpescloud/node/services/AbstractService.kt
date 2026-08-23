package de.vulpescloud.node.services

import de.vulpescloud.api.services.Service
import java.nio.file.Path

interface AbstractService {
    val service: Service

    fun start()

    fun stop()

    fun delete()

    fun command(command: String)

    fun restart()

    fun path(): Path
}
