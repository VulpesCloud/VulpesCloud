package de.vulpescloud.node.terminal.impl

import de.vulpescloud.node.NodeShutdown.ctrlCCloud
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.terminal.CommandReadingThread
import de.vulpescloud.node.terminal.JLineTerminal
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CommandReadingThreadImpl(
    private val terminal: JLineTerminal,
    private val config: NodeConfig,
) : CommandReadingThread, KoinComponent, Thread() {

    private val commandProvider: CommandProvider by inject()

    init {
        contextClassLoader = ClassLoader.getSystemClassLoader()
    }

    override fun run() {
        while (!isInterrupted) {
            try {
                try {
                    try {
                        val rawLine =
                            terminal.lineReader
                                .readLine(TerminalColorUtil.replaceColorCodes(prompt()))
                                .trim()

                        if (rawLine.isEmpty()) {
                            continue
                        }

                        //                        if (Node.instance.setupProvider.currentSetup !=
                        // null) {
                        //                            if (rawLine.equals("exit", true)) {
                        //                                Node.instance.setupProvider.cancelSetup()
                        //                                continue
                        //                            }
                        //                            Node.instance.setupProvider.input(rawLine)
                        //                        } else {
                        commandProvider.execute(CommandSource.console(), rawLine)
                        //                        }

                    } catch (ignore: EndOfFileException) {}
                } catch (exception: UserInterruptException) {
                    ctrlCCloud()
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    private fun prompt(): String {
        return ("&9" + config.name()) + "&8@&7cloud &8» &7"
    }

    override fun startThread() {
        this.start()
    }
}
