package io.github.thecguygithub.node.terminal.util

import io.github.thecguygithub.node.terminal.JLineTerminalColor



object TerminalColorUtil {

    private const val COLOR_INDEX = "&"

    fun replaceColorCodes(message: String): String {
        var updatedMessage = message
        for (color in JLineTerminalColor.entries) {
            updatedMessage = updatedMessage.replace(COLOR_INDEX + color.key, color.ansiCode)
        }
        return updatedMessage
    }
}