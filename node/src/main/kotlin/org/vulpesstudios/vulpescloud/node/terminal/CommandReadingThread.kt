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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.NodeCoroutineScope
import org.vulpesstudios.vulpescloud.node.command.CommandSource
import kotlin.system.exitProcess

class CommandReadingThread(private val terminal: Terminal) : Thread() {

    override fun run() {
        while (!isInterrupted) {
            try {
                try {
                    try {
                        val rawLine =
                            terminal.lineReader.readLine(terminal.replaceColors(prompt())).trim()

                        if (rawLine.isEmpty()) {
                            continue
                        }

                        val setupProvider = Node.instance.setupProvider

                        if (setupProvider.currentSetup != null) {
                            if (rawLine.equals("exit", true) || rawLine.equals("cancel", true) || rawLine.equals("stop", true)) {
                                setupProvider.cancelSetup()
                                continue
                            }
                            NodeCoroutineScope.launch(Dispatchers.IO) {
                                setupProvider.input(rawLine)
                            }
                        } else {
                            Node.instance.commandProvider.execute(CommandSource.CONSOLE, rawLine)
                        }

                    } catch (_: EndOfFileException) {}
                } catch (_: UserInterruptException) {
                    exitProcess(0)
                    // ctrlCCloud()
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
