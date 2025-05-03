package de.vulpescloud.node

import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.lang.Translator
import de.vulpescloud.api.player.PlayerProvider
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.api.template.TemplateStorageProvider
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.module.ModuleProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object VulpesNode : KoinComponent {

    val serviceProvider: ServiceProvider by inject()
    val taskProvider: TaskProvider by inject()
    val clusterProvider: ClusterProvider by inject()
    val playerProvider: PlayerProvider by inject()
    val moduleProvider: ModuleProvider by inject()
    val eventManager: EventManager by inject()
    val nodeConfig: NodeConfig by inject()
    val translator: Translator by inject()
    val templateStorageProvider: TemplateStorageProvider by inject()
    val versionProvider: VersionProvider by inject()

}
