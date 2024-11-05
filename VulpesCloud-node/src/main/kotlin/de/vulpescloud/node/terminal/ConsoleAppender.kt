package de.vulpescloud.node.terminal

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import de.vulpescloud.node.Node

class ConsoleAppender : ConsoleAppender<ILoggingEvent>() {

    override fun append(eventObject: ILoggingEvent) {
        Node.terminal!!.printLine(String(super.encoder.encode(eventObject)))
    }

}