package de.vulpescloud.api.service

import java.util.UUID

interface ServiceProvider {

    fun getServiceByName(name: String): ServiceInfo?

    fun getServiceByUUID(uuid: UUID): ServiceInfo?

    fun services(): List<ServiceInfo>

    fun getServicesByFilter(filter: ServiceFilter): List<ServiceInfo>

}
