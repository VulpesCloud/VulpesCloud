package de.vulpescloud.node.event.listeners.module

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.modules.ModuleLoadEvent
import de.vulpescloud.api.lang.Translator
import org.slf4j.LoggerFactory

class ModuleLoadEventListener(private val translator: Translator, private val clusterProvider: ClusterProvider) {

    private val logger = LoggerFactory.getLogger(ModuleLoadEventListener::class.java)

    @EventListener
    fun handleModuleLoadEventListener(event: ModuleLoadEvent) {
        if (event.node == clusterProvider.localNode()) {
            logger.info(translator.trans("EVENTS.ModuleLoadEvent.MESSAGE"), event.module.name)
        }
    }
}
