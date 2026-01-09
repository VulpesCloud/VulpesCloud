package de.vulpescloud.node.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import de.vulpescloud.node.Node

class ConsoleAppender : ConsoleAppender<ILoggingEvent>() {

    override fun append(eventObject: ILoggingEvent) {

        val debugLogging = System.getProperty("debugLogging").toBoolean()

        if (eventObject.level == Level.DEBUG || eventObject.level == Level.TRACE) {
            if (debugLogging) {
                Node.instance.terminal.print(String(super.encoder.encode(eventObject)))
            }
        } else {
            Node.instance.terminal.print(String(super.encoder.encode(eventObject)))
        }
    }
}
