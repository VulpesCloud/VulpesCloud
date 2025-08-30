package de.vulpescloud.node.services.impl.docker

import de.vulpescloud.api.services.Service
import de.vulpescloud.node.services.AbstractService
import java.nio.file.Path

class DockerServices(override val service: Service) : AbstractService {
    override fun start() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun delete() {
        TODO("Not yet implemented")
    }

    override fun command(command: String) {
        TODO("Not yet implemented")
    }

    override fun restart() {
        TODO("Not yet implemented")
    }

    fun path(): Path {
        return if (service.task.staticServices) {
            Path.of("local/services/${service.task.name}-${service.orderedId}")
        } else {
            Path.of("temp/services/docker/${service.task.name}-${service.orderedId}")
        }
    }
}