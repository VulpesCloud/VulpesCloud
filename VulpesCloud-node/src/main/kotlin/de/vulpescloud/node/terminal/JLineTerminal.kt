package de.vulpescloud.node.terminal

import ch.qos.logback.classic.spi.ILoggingEvent
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeConfig
import de.vulpescloud.node.terminal.util.TerminalColorUtil
import io.github.thecguygithub.api.log.LogOutputStream
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

class JLineTerminal(config: NodeConfig) {

    private val log = LoggerFactory.getLogger(JLineTerminal::class.java)
    val logLines: MutableList<ILoggingEvent> = mutableListOf()
    var terminal: Terminal
    var lineReader: LineReaderImpl
    val commandReadingThread: JLineCommandReadingThread

    init {
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

        commandReadingThread = JLineCommandReadingThread(config, this)

        System.setErr(LogOutputStream.forWarn(log).toPrintStream())
        System.setOut(LogOutputStream.forInfo(log).toPrintStream())

        clear()
        this.print(this, config)
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
        if (Node.setupProvider.currentSetup == null) {
            terminal.puts(InfoCmp.Capability.carriage_return)
            terminal.writer()
                .println(TerminalColorUtil.replaceColorCodes(message) + Ansi.ansi().a(Ansi.Attribute.RESET).toString())
            terminal.flush()
            update()
        }
    }

    fun printSetup(message: String) {
        if (Node.setupProvider.currentSetup != null) {
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

    fun print(terminal: JLineTerminal, config: NodeConfig) {
        terminal.printLine("")
        terminal.printLine("   &oVulpesCloud &8- &71.0.0-alpha")
        terminal.printLine("        &8[&OFennek&8]")
        terminal.printLine("")
    }
}