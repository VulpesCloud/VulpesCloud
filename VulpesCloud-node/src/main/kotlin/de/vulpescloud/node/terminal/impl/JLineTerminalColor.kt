package de.vulpescloud.node.terminal.impl

import org.jetbrains.annotations.Contract
import org.jline.jansi.Ansi

enum class JLineTerminalColor(internal val key: Char, internal val string: String, ansiCode: Ansi) {

    MAGENTA('m', "<magenta>", Ansi.ansi().reset().fg(Ansi.Color.MAGENTA)),
    GREEN('2', "<green>", Ansi.ansi().reset().fg(Ansi.Color.GREEN)),
    GRAY('7', "<gray>", Ansi.ansi().reset().fg(Ansi.Color.WHITE)),
    DARK_GRAY('8', "<dark_gray>", Ansi.ansi().reset().fg(Ansi.Color.BLACK).bold()),
    BLUE('9', "<blue>", Ansi.ansi().reset().fg(Ansi.Color.CYAN)),
    CYAN('b', "<cyan>", Ansi.ansi().reset().fg(Ansi.Color.CYAN).bold()),
    YELLOW('e', "<yellow>", Ansi.ansi().reset().fg(Ansi.Color.YELLOW)),
    RED('c', "<red>", Ansi.ansi().reset().fg(Ansi.Color.RED).bold()),
    WHITE('f', "<white>", Ansi.ansi().reset().fg(Ansi.Color.WHITE).bold()),
    ORANGE('o', "<orange>", Ansi.ansi().reset().fg(214)),
    BOLD_ORANGE('O', "<bold_orange>", Ansi.ansi().reset().fg(214).bold()),
    ARCTIC('A', "<arctic>", Ansi.ansi().reset().fg(123).bold());

    val ansiCode: String = ansiCode.toString()

    @Contract(pure = true)
    fun code(): String {
        return "&$key"
    }
}