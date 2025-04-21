package de.vulpescloud.api.service

import java.util.UUID

interface ServiceProvider {

    fun getServiceByName(name: String): Service?

    fun getServiceByUUID(uuid: UUID): Service?

    fun services(): List<Service>

}
