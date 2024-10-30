package io.github.thecguygithub.node.terminal

import io.github.thecguygithub.api.log.LogOutputStream
import io.github.thecguygithub.node.NodeConfig
import io.github.thecguygithub.node.terminal.handler.ConsoleTabCompleteHandler
import io.github.thecguygithub.node.terminal.util.TerminalColorUtil
import lombok.Getter
import lombok.NonNull
import lombok.experimental.Accessors
import lombok.extern.log4j.Log4j2
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@Getter
@Accessors(fluent = true)
@Log4j2
class JLineTerminal(config: NodeConfig) {


    private val log = LoggerFactory.getLogger(JLineTerminal::class.java)

    var terminal: Terminal

    var lineReader: LineReaderImpl

    val commandReadingThread: JLineCommandReadingThread

    private val tabCompleteHandler: Map<UUID, ConsoleTabCompleteHandler> =
        ConcurrentHashMap<UUID, ConsoleTabCompleteHandler>()

    init {
        terminal = TerminalBuilder.builder()
            .system(true)
            .encoding(StandardCharsets.UTF_8)
            .dumb(true)
            .jansi(true)
            .build()

        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
//            .completer(JLineCompleter())
//            .completionMatcher(JLineCompletionMatcher())

            .option(LineReader.Option.AUTO_MENU_LIST, true)
            .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
            .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "bg:default")
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .option(LineReader.Option.AUTO_PARAM_SLASH, false)
            .variable(LineReader.BELL_STYLE, "none")
            .build() as LineReaderImpl

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

    fun update() {
        if (lineReader.isReading) {
            lineReader.callWidget(LineReader.REDRAW_LINE)
            lineReader.callWidget(LineReader.REDISPLAY)
        }
    }

    fun printLine(message: String) {
        terminal.puts(InfoCmp.Capability.carriage_return)
        terminal.writer().println(TerminalColorUtil.replaceColorCodes(message) + Ansi.ansi().a(Ansi.Attribute.RESET).toString())
        terminal.flush()
        update()
    }

    fun close() {
        terminal.close()
    }

    fun updatePrompt(prompt: String) {
        lineReader.setPrompt(TerminalColorUtil.replaceColorCodes(prompt))
    }

//    fun hasSetup(): Boolean {
//        return setup != null
//    }

    fun print(terminal: JLineTerminal, config: NodeConfig) {
        terminal.printLine("")
        terminal.printLine("   &fTestCloud &8- &71.0.0-alpha")
        terminal.printLine(
            ("   &7Local node&8: &7" + config.nodes)
        )
        terminal.printLine("")
    }

    @NonNull
    fun tabCompleteHandlers(): Map<UUID, ConsoleTabCompleteHandler> {
        return this.tabCompleteHandler
    }
}