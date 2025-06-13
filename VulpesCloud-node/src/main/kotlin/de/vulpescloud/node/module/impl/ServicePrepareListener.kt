package de.vulpescloud.node.module.impl

import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.node.VulpesNode
import de.vulpescloud.node.service.LocalService
import de.vulpescloud.node.service.ServiceProviderImpl
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ServicePrepareListener {

    private val serviceProvider = VulpesNode.serviceProvider as ServiceProviderImpl
    private val moduleProvider = VulpesNode.moduleProvider as ModuleProviderImpl

    @EventListener
    fun onServiceStateChangeEvent(event: ServiceStateChangeEvent) {
        if (event.newState == ServiceStates.PREPARED) {
            val abstractService =
                serviceProvider.localServices.find { it.name == event.serviceInfo.name }
            if (abstractService != null) {
                if (abstractService !is LocalService) return
                moduleProvider.modules().forEach {
                    if (
                        it.copyToServices &&
                            it.platforms.contains(event.serviceInfo.task.version.name.lowercase())
                    ) {
                        Files.copy(
                            moduleProvider.moduleFolder.resolve("${it.name}.jar"),
                            abstractService
                                .path()
                                .resolve(event.serviceInfo.task.version.pluginDir)
                                .resolve("${it.name}.jar"),
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    }
                }
            }
        }
    }
}
