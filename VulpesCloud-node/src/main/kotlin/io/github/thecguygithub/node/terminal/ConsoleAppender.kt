package io.github.thecguygithub.node.terminal

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import io.github.thecguygithub.node.Node

class ConsoleAppender : ConsoleAppender<ILoggingEvent>() {

    override fun append(eventObject: ILoggingEvent) {
        Node.terminal!!.printLine(String(super.encoder.encode(eventObject)))
    }

}