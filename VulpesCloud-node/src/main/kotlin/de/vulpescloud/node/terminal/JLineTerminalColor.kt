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
    WHITE('f', Ansi.ansi().reset().fg(Ansi.Color.WHITE).bold()),
    ORANGE('o', Ansi.ansi().reset().fg(214)),
    BOLD_ORANGE('O', Ansi.ansi().reset().fg(214).bold());

    val ansiCode: String = ansiCode.toString()

    @Contract(pure = true)
    fun code(): String {
        return "&$key"
    }
}