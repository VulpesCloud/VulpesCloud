package de.vulpescloud.node.terminal

import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeConfig
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.command.source.CommandSource
import de.vulpescloud.node.terminal.util.TerminalColorUtil
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException

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

                        if (Node.setupProvider.currentSetup != null) {
                            if (rawLine.equals("exit", true)) {
                                Node.setupProvider.cancelSetup()
                                continue
                            }
                            Node.setupProvider.input(rawLine)
                        } else {
                            Node.commandProvider?.execute(CommandSource.console(), rawLine)
                        }

                    } catch (ignore: EndOfFileException) {}
                } catch (exception: UserInterruptException) {
                    NodeShutdown.nodeShutdown(true)
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    private fun prompt(): String {

        return ("&9" + localNodeImpl.name) + "&8@&7cloud &8» &7"

    }
}