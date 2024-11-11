package de.vulpescloud.node.logging

import de.vulpescloud.node.Node
import de.vulpescloud.node.config.LogLevels
import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.terminal.util.TerminalColorUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Logger {

    companion object {
        lateinit var instance: Logger
    }

    private var terminal: JLineTerminal = Node.terminal!!

    private val now = LocalDateTime.now()
    private val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss.SSS")
    private val formattedDateTime = now.format(formatter)


    private val infoTemplate = "&8[&7${formattedDateTime}&8] &2INFO  &8: &7 "
    private val warnTemplate = "&8[&7${formattedDateTime}&8] &eWARN  &8: &7 "
    private val errorTemplate = "&8[&7${formattedDateTime}&8] &cERROR &8: &7 "
    private val debugTemplate = "&8[&7${formattedDateTime}&8] &mDEBUG &8: &7 "

    init {
        instance = this
    }

    fun debug(message: Any) {
        if (Node.nodeConfig?.logLevel == LogLevels.DEBUG) {
            terminal.printLine(TerminalColorUtil.replaceColorCodes(debugTemplate + message))
        }
    }

    fun info(message: Any) {
        if (Node.nodeConfig?.logLevel == LogLevels.INFO || Node.nodeConfig?.logLevel == LogLevels.DEBUG) {
            terminal.printLine(TerminalColorUtil.replaceColorCodes(infoTemplate + message))
        }
    }

    fun warn(message: Any) {
        if (Node.nodeConfig?.logLevel == LogLevels.INFO || Node.nodeConfig?.logLevel == LogLevels.DEBUG || Node.nodeConfig?.logLevel == LogLevels.WARN) {
            terminal.printLine(TerminalColorUtil.replaceColorCodes(warnTemplate + message))
        }
    }

    fun error(message: Any) {
        if (Node.nodeConfig?.logLevel == LogLevels.INFO || Node.nodeConfig?.logLevel == LogLevels.DEBUG || Node.nodeConfig?.logLevel == LogLevels.WARN || Node.nodeConfig?.logLevel == LogLevels.ERROR) {
            terminal.printLine(TerminalColorUtil.replaceColorCodes(errorTemplate + message))
        }
    }
}