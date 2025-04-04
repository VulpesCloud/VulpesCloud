package de.vulpescloud.node.terminal

import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal

interface JLineTerminal {

    var terminal: Terminal
    var lineReader: LineReaderImpl
    var commandReadingThread: CommandReadingThread

    fun initTerminal()

    fun allowInput()

    fun clear()

    fun printLine(line: String)

    fun close()

    fun updatePrompt(prompt: String)

}