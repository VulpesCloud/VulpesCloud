package de.vulpescloud.node.services.impl.docker

import com.google.protobuf.Timestamp
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.services.AbstractService

class DockerServices(service: Service) : AbstractService {
    override val task = service.task
    override val uuid = service.uuid
    override val orderedId = service.orderedId
    override val port = service.port
    override val node = service.node
    override val playerCount = service.playerCount
    override val startTime: Timestamp = service.startTime
    override val state = service.state

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
}