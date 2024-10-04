package io.github.thecguygithub.node.logging

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.terminal.JLineTerminal
import io.github.thecguygithub.node.terminal.util.TerminalColorUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Logger {

    private var terminal: JLineTerminal = Node.terminal!!

    private val now = LocalDateTime.now()
    private val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss.SSS")
    private val formattedDateTime = now.format(formatter)


    private val infoTemplate = "&8[&7${formattedDateTime}&8] &2INFO &8: &7 "
    private val warnTemplate = "&8[&7${formattedDateTime}&8] &eWARN &8: &7 "
    private val errorTemplate = "&8[&7${formattedDateTime}&8] &cERROR &8: &7 "

    fun info(message: String) {
        terminal.printLine(TerminalColorUtil.replaceColorCodes(infoTemplate + message))
    }

    fun warn(message: String) {
        terminal.printLine(TerminalColorUtil.replaceColorCodes(warnTemplate + message))
    }

    fun error(message: String) {
        terminal.printLine(TerminalColorUtil.replaceColorCodes(errorTemplate + message))
    }

}