package de.vulpescloud.node.terminal

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.CommandSource
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
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

                        //                        if (setupProvider.currentSetup != null) {
                        //                            if (rawLine.equals("exit", true)) {
                        //                                setupProvider.cancelSetup()
                        //                                continue
                        //                            }
                        //                            setupProvider.input(rawLine)
                        //                        } else {
                        //
                         Node.instance.commandProvider.execute(CommandSource.CONSOLE, rawLine)
                        //                        }

                    } catch (ignore: EndOfFileException) {}
                } catch (exception: UserInterruptException) {
                    exitProcess(0)
                    // ctrlCCloud()
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    private fun prompt(): String {
        // return ("&9" + config.name()) + "&8@&7cloud &8» &7"
        return ("&9" + "DEVELOPMENT") + "&8@&7cloud &8» &7"
    }

    fun startThread() {
        this.start()
    }
}
