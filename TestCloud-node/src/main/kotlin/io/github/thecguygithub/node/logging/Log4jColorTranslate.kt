package io.github.thecguygithub.node.logging

import io.github.thecguygithub.node.terminal.JLineTerminalColor
import org.apache.logging.log4j.Level


object Log4jColorTranslate {

    fun translate(level: Level): String {
        if (level === Level.WARN) {
            return JLineTerminalColor.YELLOW.code()
        }
        if (level === Level.INFO) {
            return JLineTerminalColor.CYAN.code()
        }
        if (level === Level.ERROR) {
            return JLineTerminalColor.RED.code()
        }
        if (level === Level.DEBUG) {
            return JLineTerminalColor.WHITE.code()
        }
        if (level === Level.FATAL) {
            return JLineTerminalColor.RED.code()
        }
        return JLineTerminalColor.GRAY.code()
    }
}