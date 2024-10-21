package io.github.thecguygithub.api.tasks

import io.github.thecguygithub.api.Detail
import io.github.thecguygithub.api.Named
import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.api.version.VersionInfo



interface Task : Named, Detail {

    fun nodes(): List<String?>

    fun templates(): List<String?>

    fun maxMemory(): Int

    fun maxPlayers(): Int

    fun staticService(): Boolean

    fun minOnlineCount(): Int

    fun serviceCount(): Long?

    fun services(): List<ClusterService?>?

    fun version(): VersionInfo

    fun maintenance(): Boolean

    fun startPort(): Int

    fun update()

}