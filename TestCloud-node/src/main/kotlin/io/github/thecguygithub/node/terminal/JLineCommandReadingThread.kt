package io.github.thecguygithub.node.terminal

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.NodeConfig
import io.github.thecguygithub.node.NodeShutdown
import io.github.thecguygithub.node.terminal.util.TerminalColorUtil
import lombok.extern.slf4j.Slf4j
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import java.util.Arrays


@Slf4j
class JLineCommandReadingThread(private val localNodeImpl: NodeConfig, private val terminal: JLineTerminal) : Thread() {


    init {
        contextClassLoader = ClassLoader.getSystemClassLoader()
    }

    override fun run() {

        while (!isInterrupted) {

            try {

                try {

                    try {

                        val rawLine = terminal.lineReader.readLine(TerminalColorUtil.replaceColorCodes(prompt())).trim()

                        if (rawLine.isEmpty()) {
                            continue
                        }

                        val line = rawLine.split(" ")



                        Node.commandProvider!!.call(line[0], Arrays.copyOfRange(line.toTypedArray(), 1, line.size))

                    } catch (ignore: EndOfFileException) {

                    }

                } catch (exception: UserInterruptException) {
                    NodeShutdown.nodeShutdown(true)
                }

            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    private fun prompt(): String {

        return ("&9" + localNodeImpl.localNode).toString() + "&8@&7cloud &8» &7"

    }
}