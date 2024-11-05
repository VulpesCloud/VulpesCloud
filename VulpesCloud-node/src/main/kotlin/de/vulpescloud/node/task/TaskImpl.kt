package de.vulpescloud.node.task

import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.version.VersionInfo

open class TaskImpl(
    val name: String,
    val maxMemory: Int,
    val version: VersionInfo,
    val templates: List<String?>,
    val nodes: List<String?>,
    val maxPlayers: Int,
    val staticServices: Boolean,
    val minOnlineCount: Int,
    val maintenance: Boolean,
    val startPort: Int,
    val fallback: Boolean
) : Task {

    override fun details(): String? {
        return ""
    }

    override fun maintenance(): Boolean {
        return maintenance
    }

    override fun maxMemory(): Int {
        return maxMemory
    }

    override fun maxPlayers(): Int {
        return maxPlayers
    }

    override fun minOnlineCount(): Int {
        return minOnlineCount
    }

    override fun name(): String {
        return name
    }

    override fun nodes(): List<String?> {
        return nodes
    }

    override fun version(): VersionInfo {
        return version
    }

    override fun serviceCount(): Long? {
        return null
    }

    override fun services(): List<ClusterService?>? {
        return null
    }

    override fun startPort(): Int {
        return startPort
    }

    override fun staticService(): Boolean {
        return staticServices
    }

    override fun templates(): List<String?> {
        return templates
    }

    override fun update() {
        TODO("Not yet implemented")
    }

    override fun fallback(): Boolean {
        return fallback
    }
}