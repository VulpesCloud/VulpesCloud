package de.vulpescloud.node.event.listeners.module

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.modules.ModuleStartEvent
import de.vulpescloud.api.lang.Translator
import org.slf4j.LoggerFactory

class ModuleStartEventListener(
    private val translator: Translator,
    private val clusterProvider: ClusterProvider,
) {

    private val logger = LoggerFactory.getLogger(ModuleStartEventListener::class.java)

    @EventListener
    fun handleModuleStartEventListener(event: ModuleStartEvent) {
        if (event.node == clusterProvider.localNode()) {
            logger.info(translator.trans("EVENTS.ModuleStart.MESSAGE"), event.module.name)
        }
    }
}
