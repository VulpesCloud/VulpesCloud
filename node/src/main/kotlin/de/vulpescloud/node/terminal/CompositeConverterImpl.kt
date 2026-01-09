package de.vulpescloud.node.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter

class CompositeConverterImpl : CompositeConverter<ILoggingEvent>() {

    override fun transform(p0: ILoggingEvent, p1: String): String {
        return color(p0.level) + p1
    }

    private fun color(level: Level): String {
        return when (level.toInt()) {
            Level.INFO_INT -> {
                "<color:#4bfb00>"
            }
            Level.WARN_INT -> {
                "<color:#fede00>"
            }
            Level.ERROR_INT -> {
                "<color:#ff0000>"
            }
            Level.TRACE_INT -> {
                "<color:#7F00FF>"
            }
            Level.DEBUG_INT -> {
                "<color:#7F00FF>"
            }
            else -> {
                "<dark_gray>"
            }
        }
    }
}
