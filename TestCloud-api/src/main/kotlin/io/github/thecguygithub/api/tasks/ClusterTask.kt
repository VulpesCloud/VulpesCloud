package io.github.thecguygithub.api.tasks

import io.github.thecguygithub.api.Detail
import io.github.thecguygithub.api.Named
import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.services.ClusterService


interface ClusterTask : Named, Detail {

    fun nodes(): Array<String?>?

    fun templates(): Array<String?>?

    fun maxMemory(): Int

    fun maxPlayers(): Int

    fun staticService(): Boolean

    fun minOnlineCount(): Int

    fun serviceCount(): Long

    fun services(): List<ClusterService?>?

    fun platform(): PlatformGroupDisplay?

    fun maintenance(): Boolean

    fun fallback(): Boolean {
        return this is FallbackClusterTask
    }

    fun update()

}