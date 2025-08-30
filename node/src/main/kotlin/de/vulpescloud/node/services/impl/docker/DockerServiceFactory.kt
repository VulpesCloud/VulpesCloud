package de.vulpescloud.node.services.impl.docker

import de.vulpescloud.api.services.Service
import de.vulpescloud.node.services.AbstractService
import de.vulpescloud.node.services.AbstractServiceFactory

class DockerServiceFactory : AbstractServiceFactory() {
    override suspend fun prepareService(service: Service): AbstractService {
        TODO("Not yet implemented")
    }
}