package de.vulpescloud.node.terminal.impl

object TerminalColorUtil {

    fun replaceColorCodes(message: String): String {
        var msg = message
        for (color in JLineTerminalColor.entries) {
            msg = msg.replace("&${color.key}", color.ansiCode)
            msg = msg.replace(color.string, color.ansiCode)
        }
        return msg
    }

}