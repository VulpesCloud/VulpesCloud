package de.vulpescloud.node.terminal.impl

import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.terminal.CommandReadingThread
import de.vulpescloud.node.terminal.JLineTerminal
import java.nio.charset.StandardCharsets
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp

class JLineTerminalImpl(config: NodeConfig, private val setupProvider: SetupProvider) :
    JLineTerminal {

    override lateinit var terminal: Terminal
    override lateinit var lineReader: LineReaderImpl
    override var commandReadingThread: CommandReadingThread = CommandReadingThreadImpl(this, config)

    override fun initTerminal() {
        terminal =
            TerminalBuilder.builder()
                .system(true)
                .encoding(StandardCharsets.UTF_8)
                .dumb(true)
                .jansi(true)
                .build()

        lineReader =
            LineReaderBuilder.builder()
                .terminal(terminal)
                // .completer(JLineCompleter())

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
        this.print()
    }

    override fun allowInput() {
        commandReadingThread.startThread()
    }

    override fun clear() {
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

    override fun printLine(line: String) {
        if (setupProvider.currentSetup == null) {
            terminal.puts(InfoCmp.Capability.carriage_return)
            terminal
                .writer()
                .println(
                    TerminalColorUtil.replaceColorCodes(line) +
                            Ansi.ansi().a(Ansi.Attribute.RESET).toString()
                )
            terminal.flush()
            update()
        }
    }

    override fun printSetup(line: String) {
        if (setupProvider.currentSetup != null) {
            terminal.puts(InfoCmp.Capability.carriage_return)
            terminal
                .writer()
                .println(
                    TerminalColorUtil.replaceColorCodes(line) +
                            Ansi.ansi().a(Ansi.Attribute.RESET).toString()
                )
            terminal.flush()
            update()
        }
    }

    override fun close() {
        terminal.close()
    }

    override fun updatePrompt(prompt: String) {
        lineReader.setPrompt(TerminalColorUtil.replaceColorCodes(prompt))
    }

    private fun print() {
        printLine("")
        printLine("   &oVulpesCloud &8- &72.0.0")
        printLine("        &8[&OFennek&8]")
        printLine("")
    }
}
