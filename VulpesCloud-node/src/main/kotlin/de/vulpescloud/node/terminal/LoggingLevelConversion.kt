/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
            Level.DEBUG_INT -> { JLineTerminalColor.MAGENTA.key.toString() }
            else -> { JLineTerminalColor.DARK_GRAY.key.toString() }
        }
    }

}