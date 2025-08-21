package de.vulpescloud.node.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter
import net.kyori.adventure.text.format.NamedTextColor

class CompositeConverterImpl : CompositeConverter<ILoggingEvent>() {

    override fun transform(p0: ILoggingEvent, p1: String): String {
        return "&" + color(p0.level) + p1
    }

    private fun color(level: Level): String {
        return when (level.toInt()) {
            Level.INFO_INT -> {
                NamedTextColor.GREEN.toString()
            }
            Level.WARN_INT -> {
                NamedTextColor.YELLOW.toString()
            }
            Level.ERROR_INT -> {
                NamedTextColor.RED.toString()
            }
            Level.TRACE_INT -> {
                NamedTextColor.LIGHT_PURPLE.toString()
            }
            Level.DEBUG_INT -> {
                NamedTextColor.LIGHT_PURPLE.toString()
            }
            else -> {
                NamedTextColor.DARK_GRAY.toString()
            }
        }
    }
}
