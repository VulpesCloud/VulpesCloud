package io.github.thecguygithub.node.terminal

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.NodeConfig
import io.github.thecguygithub.node.NodeShutdown
import io.github.thecguygithub.node.command.provider.CommandProvider
import io.github.thecguygithub.node.command.source.CommandSource
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.terminal.util.TerminalColorUtil
import lombok.extern.slf4j.Slf4j
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import java.util.Arrays


@Slf4j
class   JLineCommandReadingThread(private val localNodeImpl: NodeConfig, private val terminal: JLineTerminal) : Thread() {


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

                        Node.commandProvider?.execute(CommandSource.console(), rawLine)
//                            if (terminal.setup != null) {
//                                if (rawLine.equals("exit", true)) {
//                                    terminal.setup!!.exit(false)
//                                    continue;
//                                }
//
//                                if (rawLine.equals("back", true)) {
//                                    terminal.setup!!.previousQuestion();
//                                    continue;
//                                }
//
//                                terminal.setup!!.answer(rawLine);
//
//                            } else {
//                                Node.commandProvider?.execute(CommandSource.console(), rawLine)
////                                    .commandManager?.commandExecutor()?.executeCommand(CommandSource.console(), rawLine)
//
//                            }

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

        return ("&9" + localNodeImpl.name) + "&8@&7cloud &8» &7"

    }
}