package de.vulpescloud.node.tasks

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.node.Node

data class TaskImpl(
    var name: String,
    var maxMemory: Int,
    var version: VersionInfo,
    var templates: List<String?>,
    var nodes: List<String?>,
    var maxPlayers: Int,
    var staticServices: Boolean,
    var minOnlineCount: Int,
    var maintenance: Boolean,
    var startPort: Int,
    var fallback: Boolean
) : Task {

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
        return Node.instance.serviceProvider.services().stream().filter { it.task().name() == name() }?.count()
    }

    override fun services(): List<Service?>? {
        return Node.instance.serviceProvider.services().stream().filter { it.task().name() == name() }?.toList()
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

    override fun fallback(): Boolean {
        return fallback
    }
}