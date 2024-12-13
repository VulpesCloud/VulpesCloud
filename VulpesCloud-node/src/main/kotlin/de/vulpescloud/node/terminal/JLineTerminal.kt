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

import de.vulpescloud.node.Node
import de.vulpescloud.node.terminal.util.TerminalColorUtil
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

class JLineTerminal {

    private val log = LoggerFactory.getLogger(JLineTerminal::class.java)
    lateinit var terminal: Terminal
    lateinit var lineReader: LineReaderImpl
    val commandReadingThread: JLineCommandReadingThread = JLineCommandReadingThread(this)

    fun initialize() {
        terminal = TerminalBuilder.builder()
            .system(true)
            .encoding(StandardCharsets.UTF_8)
            .dumb(true)
            .jansi(true)
            .build()

        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(JLineCompleter())

            .option(LineReader.Option.AUTO_MENU_LIST, true)
            .option(LineReader.Option.AUTO_GROUP, false)
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .option(LineReader.Option.AUTO_PARAM_SLASH, false)

            .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
            .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "bg:default")
            .variable(LineReader.BELL_STYLE, "none")

            .build() as LineReaderImpl

        lineReader.autosuggestion = LineReader.SuggestionType.COMPLETER

        clear()
        this.print(this)
    }

    fun allowInput() {
        commandReadingThread.start()
    }

    fun clear() {
        terminal.puts(InfoCmp.Capability.clear_screen)
        terminal.flush()
        update()
    }

    private fun update() {
        if (lineReader.isReading) {
            lineReader.callWidget(LineReader.REDRAW_LINE)
            lineReader.callWidget(LineReader.REDISPLAY)
        }
    }

    fun printLine(message: String) {
        if (Node.instance.setupProvider.currentSetup == null) {
            terminal.puts(InfoCmp.Capability.carriage_return)
            terminal.writer()
                .println(TerminalColorUtil.replaceColorCodes(message) + Ansi.ansi().a(Ansi.Attribute.RESET).toString())
            terminal.flush()
            update()
       }

        printSetup("aaisdhgkl >> $message")
    }

    fun printSetup(message: String) {
        if (Node.instance.setupProvider.currentSetup != null) {
            terminal.puts(InfoCmp.Capability.carriage_return)
            terminal.writer()
                .println(TerminalColorUtil.replaceColorCodes(message) + Ansi.ansi().a(Ansi.Attribute.RESET).toString())
            terminal.flush()
            update()
        }
    }

    fun close() {
        terminal.close()
    }

    fun updatePrompt(prompt: String) {
        lineReader.setPrompt(TerminalColorUtil.replaceColorCodes(prompt))
    }

    private fun print(terminal: JLineTerminal) {
        terminal.printLine("")
        terminal.printLine("   &oVulpesCloud &8- &71.0.0-alpha")
        terminal.printLine("        &8[&OFennek&8]")
        terminal.printLine("")
    }
}