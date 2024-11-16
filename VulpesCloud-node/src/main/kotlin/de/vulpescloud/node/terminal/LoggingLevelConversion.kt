package de.vulpescloud.node.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter

class LoggingLevelConversion : CompositeConverter<ILoggingEvent>() {

    override fun transform(p0: ILoggingEvent, p1: String): String {
        return "&" + color(p0.level) + p1
    }

    private fun color(level: Level): String {
        return when (level.toInt()) {
            Level.INFO_INT -> { JLineTerminalColor.GREEN.key.toString() }
            Level.WARN_INT -> { JLineTerminalColor.YELLOW.key.toString() }
            Level.ERROR_INT -> { JLineTerminalColor.RED.key.toString() }
            Level.TRACE_INT -> { JLineTerminalColor.MAGENTA.key.toString() }
            else -> { JLineTerminalColor.DARK_GRAY.key.toString() }
        }
    }

}