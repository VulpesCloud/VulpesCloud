package de.vulpescloud.node.service

import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.events.service.ServiceLogEvent
import de.vulpescloud.node.VulpesNode
import java.util.concurrent.ConcurrentHashMap

object ServiceLogBuffer {
    private val logBuffers = ConcurrentHashMap<String, MutableList<String>>()
    private const val MAX_BUFFER_SIZE = 1000

    init {
        VulpesNode.eventManager.registerListener(this)
    }

    @EventListener
    fun onServiceLog(event: ServiceLogEvent) {
        addLog(event.serviceInfo.name, event.rawMessage)
    }

    fun addLog(serviceName: String, logLine: String) {
        val buffer = logBuffers.computeIfAbsent(serviceName) { mutableListOf() }
        synchronized(buffer) {
            buffer.add(logLine)
            if (buffer.size > MAX_BUFFER_SIZE) {
                buffer.removeAt(0)
            }
        }
    }

    fun getLogs(serviceName: String, lines: Int = MAX_BUFFER_SIZE): List<String> {
        return logBuffers[serviceName]?.takeLast(lines) ?: emptyList()
    }

    fun clearLogs(serviceName: String) {
        logBuffers.remove(serviceName)
    }

    fun isBuffering(serviceName: String): Boolean {
        return logBuffers.containsKey(serviceName)
    }
}