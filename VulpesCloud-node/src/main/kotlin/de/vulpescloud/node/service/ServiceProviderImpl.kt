package de.vulpescloud.node.service

import de.vulpescloud.api.service.Service
import de.vulpescloud.api.service.ServiceProvider
import java.util.*

class ServiceProviderImpl : ServiceProvider {

    val localServices = mutableListOf<LocalService>()

    override fun getServiceByName(name: String): Service? {
        TODO("Not yet implemented")
    }

    override fun getServiceByUUID(uuid: UUID): Service? {
        TODO("Not yet implemented")
    }

    override fun services(): List<Service> {
        TODO("Not yet implemented")
    }

}
