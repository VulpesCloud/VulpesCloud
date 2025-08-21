package de.vulpescloud.node.terminal

import de.vulpescloud.node.CloudVersion
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.nio.charset.StandardCharsets

class Terminal {

    private lateinit var terminal: Terminal
    lateinit var lineReader: LineReaderImpl
    private val miniMessage = MiniMessage.miniMessage()
    private val ansiComponentSerializer = ANSIComponentSerializer.ansi()
    private val commandReadingThread = CommandReadingThread(this)

    fun init() {
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
                // .completer(JLineTabCompleter(setupProvider, commandProvider))

                .option(LineReader.Option.AUTO_MENU_LIST, true)
                .option(LineReader.Option.AUTO_GROUP, false)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .option(LineReader.Option.AUTO_PARAM_SLASH, false)
                .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
                .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "bg:default")
                .variable(LineReader.BELL_STYLE, "none")
                .build() as LineReaderImpl

        lineReader.autosuggestion = LineReader.SuggestionType.COMPLETER
        printHeader()
    }

    fun allowInput() {
        commandReadingThread.startThread()
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

    fun print(line: String) {
        terminal.puts(InfoCmp.Capability.carriage_return)
        terminal
            .writer()
            .println(
                replaceColors(line) + Ansi.ansi().a(Ansi.Attribute.RESET).toString()
            )
        terminal.flush()
        update()
    }

    fun close() {
        commandReadingThread.interrupt()
        terminal.close()
    }

    fun updatePrompt(prompt: String) {
        lineReader.setPrompt(replaceColors(prompt))
    }

    private fun printHeader() {
        print("")
        print("   <color:#ff700a>VulpesCloud</color> <dark_gray>-</dark_gray> <gray><color:#ff700a>v${CloudVersion.getVersion()}</color>#${CloudVersion.getGitCommit()}</gray>")
        print("             <dark_gray>[<color:#008ff5>Swift</color>]</dark_gray>")
        print("")
    }

    fun replaceColors(line: String): String {
        return ansiComponentSerializer.serialize(miniMessage.deserialize(convertToMinimessage(line)))
    }

    private fun convertToMinimessage(input: String): String {
        val legacySerializer =
            LegacyComponentSerializer.builder().character('&').extractUrls().hexColors().build()

        val component = legacySerializer.deserialize(input.replace("§", "&"))

        val miniMessageString = MiniMessage.miniMessage().serialize(component)

        return miniMessageString.replace("\\", "")
    }
}
