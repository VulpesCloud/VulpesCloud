/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.terminal

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
import org.vulpesstudios.vulpescloud.node.CloudVersion
import org.vulpesstudios.vulpescloud.node.Node
import java.nio.charset.StandardCharsets

class Terminal {

    lateinit var terminal: Terminal
    lateinit var lineReader: LineReaderImpl
    private val miniMessage = MiniMessage.miniMessage()
    private val ansiComponentSerializer = ANSIComponentSerializer.ansi()
    private val commandReadingThread = CommandReadingThread(this)
    val terminalContent: MutableList<String> = mutableListOf()
    var prompt = ""

    private fun getDefaultPrompt(): String {
        val nodeName = Node.instance.configProvider.config.nodeName
        val versionPart =
            if (CloudVersion.getGitBranch() == "stable") {
                "v3"
            } else {
                "dev-${CloudVersion.getGitCommit()}"
            }
        return "&f$nodeName&8@<color:#ff700a>$versionPart</color> &8» &7"
    }

    fun changePrompt(prompt: String) {
        if (prompt == "" || prompt == "default") {
            this.prompt = getDefaultPrompt()
        } else {
            this.prompt = prompt
        }
    }

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
                .completer(JLineTabCompleter(Node.instance.commandProvider))
                .option(LineReader.Option.AUTO_MENU_LIST, true)
                .option(LineReader.Option.AUTO_GROUP, false)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .option(LineReader.Option.AUTO_PARAM_SLASH, false)
                .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
                .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "bg:default")
                .option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.EMPTY_WORD_OPTIONS, false)
                .option(LineReader.Option.HISTORY_TIMESTAMPED, false)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
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
        if (Node.instance.setupProvider.currentSetup == null) {
            if (terminalContent.size > 250) terminalContent.removeFirst()
            terminalContent.add(line)
            lineReader.printAbove(replaceColors(line) + Ansi.ansi().a(Ansi.Attribute.RESET).toString())
        }
    }

    fun printSetup(line: String) {
        if (Node.instance.setupProvider.currentSetup != null) {
            lineReader.printAbove(replaceColors(line) + Ansi.ansi().a(Ansi.Attribute.RESET).toString())
        }
    }

    fun printNoCheck(line: String) {
        lineReader.printAbove(replaceColors(line) + Ansi.ansi().a(Ansi.Attribute.RESET).toString())
    }

    fun close() {
        commandReadingThread.interrupt()
        terminal.close()
    }

    private fun printHeader() {
        print("")
        print(
            "   <color:#ff700a>VulpesCloud</color> <dark_gray>-</dark_gray> <gray><color:#ff700a>v${CloudVersion.getVersion()}</color>@${CloudVersion.getGitCommit()}</gray>"
        )
        print("             <dark_gray>[<color:#008ff5>Swift</color>]</dark_gray>")
        print("")
    }

    fun replaceColors(line: String): String {
        return ansiComponentSerializer.serialize(
            miniMessage.deserialize(convertToMinimessage(line))
        )
    }

    private fun convertToMinimessage(input: String): String {
        val legacySerializer =
            LegacyComponentSerializer.builder().character('&').extractUrls().hexColors().build()

        val component = legacySerializer.deserialize(input.replace("§", "&"))

        val miniMessageString = MiniMessage.miniMessage().serialize(component)

        return miniMessageString.replace("\\", "")
    }
}
