package de.vulpescloud.node.terminal.impl

import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.impl.ConsoleCommandSource
import de.vulpescloud.node.setup.SetupProvider
import org.incendo.cloud.suggestion.Suggestion
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class JLineTabCompleter(
    private val setupProvider: SetupProvider,
    private val commandProvider: CommandProvider,
) : Completer {
    override fun complete(lineReader: LineReader, parsedLine: ParsedLine, p2: MutableList<Candidate>) {
        val line = parsedLine.line()
        val suggestions: List<String> = if (setupProvider.currentSetup != null) {
            setupProvider.getSetupAnswers(line)
        } else {
            commandProvider.commandManager.suggestionFactory().suggest(ConsoleCommandSource(), line).join().list().stream()
                .map(Suggestion::suggestion)
                .toList()
        }

        if (suggestions.isEmpty()) {
            return
        }

        val answers = ArrayList<String>()
        answers.addAll(suggestions)

        if (answers.isNotEmpty()) {
            answers.sort()
            p2.addAll(answers.map { Candidate(it) })
        }
    }
}
