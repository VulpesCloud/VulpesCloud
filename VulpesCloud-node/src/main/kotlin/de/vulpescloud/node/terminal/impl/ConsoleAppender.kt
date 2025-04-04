package de.vulpescloud.node.terminal.impl

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import de.vulpescloud.node.terminal.JLineTerminal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConsoleAppender : ConsoleAppender<ILoggingEvent>(), KoinComponent {

    private val terminal: JLineTerminal by inject()

    override fun append(eventObject: ILoggingEvent) {

        val debugLogging = System.getProperty("debugLogging").toBoolean()

        if (eventObject.level == Level.DEBUG || eventObject.level == Level.TRACE) {
            if (debugLogging) {
                terminal.printLine(String(super.encoder.encode(eventObject)))
            }
        } else {
            terminal.printLine(String(super.encoder.encode(eventObject)))
        }
    }

}