package de.vulpescloud.node.terminal.util

import de.vulpescloud.node.terminal.JLineTerminalColor



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