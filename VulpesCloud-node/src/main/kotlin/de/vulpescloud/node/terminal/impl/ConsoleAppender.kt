package de.vulpescloud.node.terminal.impl

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import de.vulpescloud.api.event.events.cluster.NodeLogEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.node.VulpesNode
import de.vulpescloud.node.event.EventManagerImpl
import de.vulpescloud.node.terminal.JLineTerminal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConsoleAppender : ConsoleAppender<ILoggingEvent>(), KoinComponent {

    private val terminal: JLineTerminal by inject()

    override fun append(eventObject: ILoggingEvent) {

        val debugLogging = System.getProperty("debugLogging").toBoolean()
        val event =
            NodeLogEvent(
                VulpesNode.clusterProvider.localNode(),
                eventObject.level.toString(),
                eventObject.message,
                eventObject.formattedMessage,
            )

        if (eventObject.level == Level.DEBUG || eventObject.level == Level.TRACE) {
            if (debugLogging) {
                terminal.printLine(String(super.encoder.encode(eventObject)))
            }
        } else {
            terminal.printLine(String(super.encoder.encode(eventObject)))
        }

        (VulpesNode.eventManager as EventManagerImpl).callGlobal(event, RedisChannels.VULPESCLOUD_EVENT_CLUSTER_NodeLogEvent)
    }
}
