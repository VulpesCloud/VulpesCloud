package io.github.thecguygithub.api.tasks

import io.github.thecguygithub.api.CloudAPI
import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.services.ClusterService
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.experimental.Accessors


@Getter
@Accessors(fluent = true)
@AllArgsConstructor
abstract class AbstractClusterGroup : ClusterTask {
    private val name: String? = null
    private val platform: PlatformGroupDisplay? = null

    private val templates: Array<String>
    private val nodes: Array<String>
    private val maxMemory = 0
    private val maxPlayers = 0
    private val staticService = false
    private val minOnlineServerInstances = 0
    private val maxOnlineServerInstances = 0

    // private val properties: PropertiesPool? = null

    fun details(): String {
        var defaultDetail: Unit =
            ("platform&8=&7" + platform.details()).toString() + "&8, &7nodes&8=&7" + nodes.contentToString() + ", &7maxMemory&8=&7" + maxMemory + "&8, &7static&8=&7" + staticService

        if (this is FallbackClusterTask) {
            defaultDetail += " &8--fallback"
        }

        return defaultDetail
    }

    override fun serviceCount(): Long {
        return CloudAPI.instance.serviceProvider().services().stream().filter { it ->
            it.group().equals(this)
        }.count()
    }

    override fun services(): List<ClusterService> {
        return CloudAPI.instance.serviceProvider().services().stream().filter { it ->
            it.group().equals(this)
        }.toList()
    }

    override fun equals(other: Any?): Boolean {
        return other is ClusterTask && other.name() == name
    }
}