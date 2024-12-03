package de.vulpescloud.api.tasks

import de.vulpescloud.api.Named
import de.vulpescloud.api.version.VersionInfo

interface Task : Named {

    fun nodes(): List<String?>

    fun templates(): List<String?>

    fun maxMemory(): Int

    fun maxPlayers(): Int

    fun staticService(): Boolean

    fun minOnlineCount(): Int

    // fun serviceCount(): Long?

    // fun services(): List<ClusterService?>?

    fun version(): VersionInfo

    fun maintenance(): Boolean

    fun startPort(): Int

    fun fallback(): Boolean

}