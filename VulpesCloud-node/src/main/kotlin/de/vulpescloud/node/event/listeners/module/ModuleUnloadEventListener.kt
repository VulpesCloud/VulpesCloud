package de.vulpescloud.node.event.listeners.module

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.modules.ModuleUnloadEvent
import de.vulpescloud.api.lang.Translator
import org.slf4j.LoggerFactory

class ModuleUnloadEventListener(
    private val translator: Translator,
    private val clusterProvider: ClusterProvider,
) {

    private val logger = LoggerFactory.getLogger(ModuleUnloadEventListener::class.java)

    @EventListener
    fun handleModuleUnloadEventListener(event: ModuleUnloadEvent) {
        if (event.node == clusterProvider.localNode()) {
            logger.info(translator.trans("EVENTS.ModuleUnloadEvent.MESSAGE"), event.module.name)
        }
    }
}
