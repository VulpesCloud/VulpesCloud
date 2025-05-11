package de.vulpescloud.node.module.impl

import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.node.VulpesNode
import de.vulpescloud.node.service.ServiceProviderImpl
import java.nio.file.Files

class ServicePrepareListener {

    private val serviceProvider = VulpesNode.serviceProvider as ServiceProviderImpl
    private val moduleProvider = VulpesNode.moduleProvider as ModuleProviderImpl

    @EventListener
    fun onServiceStateChangeEvent(event: ServiceStateChangeEvent) {
        if (event.newState == ServiceStates.PREPARED) {
            if (serviceProvider.localServices.find { it.name == event.service.name } != null) {
                moduleProvider.modules().forEach {
                    if (
                        it.copyToServices &&
                            it.platforms.contains(event.service.task.version.name.lowercase())
                    ) {
                        Files.copy(
                            moduleProvider.moduleFolder.resolve("${it.name}.jar"),
                            event.service
                                .path()
                                .resolve(event.service.task.version.pluginDir)
                                .resolve("${it.name}.jar"),
                        )
                    }
                }
            }
        }
    }
}
