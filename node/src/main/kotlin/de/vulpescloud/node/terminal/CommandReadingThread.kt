package de.vulpescloud.node.terminal

import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.NodeShutdown
import de.vulpescloud.node.command.CommandSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException

class CommandReadingThread(private val terminal: Terminal) : Thread() {

    override fun run() {
        while (!isInterrupted) {
            try {
                try {
                    try {
                        try {
                            val rawLine =
                                terminal.lineReader
                                    .readLine(terminal.replaceColors(prompt()))
                                    .trim()

                            if (rawLine.isEmpty()) {
                                continue
                            }

                            val setupProvider = Node.instance.setupProvider

                            if (setupProvider.currentSetup != null) {
                                if (
                                    rawLine.equals("exit", true) ||
                                        rawLine.equals("cancel", true) ||
                                        rawLine.equals("stop", true)
                                ) {
                                    setupProvider.cancelSetup()
                                    continue
                                }
                                NodeCoroutineScope.launch(Dispatchers.IO) {
                                    setupProvider.input(rawLine)
                                }
                            } else {
                                NodeCoroutineScope.launch(Dispatchers.IO) {
                                    Node.instance.commandProvider.execute(
                                        CommandSource.CONSOLE,
                                        rawLine,
                                    )
                                }
                            }
                        } catch (_: IllegalStateException) {}
                    } catch (_: EndOfFileException) {}
                } catch (_: UserInterruptException) {
                    runBlocking {
                        NodeShutdown.shutdown()
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    private fun prompt(): String {
        return terminal.prompt
    }

    fun startThread() {
        this.start()
    }
}
