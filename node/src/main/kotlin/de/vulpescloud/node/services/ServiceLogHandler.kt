package de.vulpescloud.node.services

import build.buf.gen.vulpescloud.events.v1.ServiceLogEvent
import de.vulpescloud.api.services.Service
import de.vulpescloud.node.Node
import de.vulpescloud.node.event.EventsService
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

object ServiceLogHandler {
    private val logBuffers = ConcurrentHashMap<String, MutableList<String>>()
    private val servicesToLog = mutableListOf<String>()
    private const val MAX_BUFFER_SIZE = 250
    private val logger = LoggerFactory.getLogger("ServiceLogBuffer")
    private var subscribeJob: Job? = null

    fun subscribe() {
        subscribeJob =
            EventsService.subscribe<ServiceLogEvent> { event ->
                addLog("${event.service.task.name}-${event.service.orderedId}", event.message)
                if (
                    servicesToLog.contains("${event.service.task.name}-${event.service.orderedId}")
                ) {
                    logLine(Service.fromDefinition(event.service).name(), event.message)
                }
            }
    }

    fun unsubscribe() {
        subscribeJob?.cancel()
        subscribeJob = null
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

    fun toggleServiceLogging(serviceName: String) {
        if (servicesToLog.contains(serviceName)) {
            servicesToLog.remove(serviceName)
            logger.info("<gray>Logging for service</gray> <white>$serviceName</white> <red>disabled</red><dark_gray>.</dark_gray>")
        } else {
            servicesToLog.add(serviceName)
            getLogs(serviceName).forEach { logLine(serviceName, it) }
            logger.info("<gray>Logging for service</gray> <white>$serviceName</white> <green>enabled</green><dark_gray>.</dark_gray>")
        }
    }

    private fun logLine(serviceName: String, line: String) {
        if (Node.instance.configProvider.config.testing.newServiceLoggingStyle) {
            println("<dark_gray>[</dark_gray><white>$serviceName</white><dark_gray>]</dark_gray> <gray>$line</gray>")
        } else {
            logger.info("[$serviceName] $line")
        }
    }
}
