package de.vulpescloud.bridge

import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.bridge.service.ServiceProviderImpl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object VulpesBridge : KoinComponent {

    private val serviceProvider: ServiceProvider by inject()

    fun getServiceProvider(): ServiceProviderImpl = serviceProvider as ServiceProviderImpl

}
