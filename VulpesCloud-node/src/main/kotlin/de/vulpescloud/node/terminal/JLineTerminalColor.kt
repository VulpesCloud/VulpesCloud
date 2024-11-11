package de.vulpescloud.node.terminal

import lombok.AllArgsConstructor
import lombok.Getter
import lombok.experimental.Accessors
import org.jetbrains.annotations.Contract
import org.jline.jansi.Ansi


@Getter
@Accessors(fluent = true)
@AllArgsConstructor
enum class JLineTerminalColor(internal val key: Char, ansiCode: Ansi) {

    MAGENTA('m', Ansi.ansi().reset().fg(Ansi.Color.MAGENTA)),
    GREEN('2', Ansi.ansi().reset().fg(Ansi.Color.GREEN)),
    GRAY('7', Ansi.ansi().reset().fg(Ansi.Color.WHITE)),
    DARK_GRAY('8', Ansi.ansi().reset().fg(Ansi.Color.BLACK).bold()),
    BLUE('9', Ansi.ansi().reset().fg(Ansi.Color.CYAN)),
    CYAN('b', Ansi.ansi().reset().fg(Ansi.Color.CYAN).bold()),
    YELLOW('e', Ansi.ansi().reset().fg(Ansi.Color.YELLOW)),
    RED('c', Ansi.ansi().reset().fg(Ansi.Color.RED).bold()),
    WHITE('f', Ansi.ansi().reset().fg(Ansi.Color.WHITE).bold());

    val ansiCode: String = ansiCode.toString()

    @Contract(pure = true)
    fun code(): String {
        return "&$key"
    }
}