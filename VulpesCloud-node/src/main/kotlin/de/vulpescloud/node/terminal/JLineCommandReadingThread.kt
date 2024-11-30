/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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