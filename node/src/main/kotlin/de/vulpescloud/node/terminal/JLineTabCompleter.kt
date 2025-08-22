package de.vulpescloud.node.terminal

import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.ConsoleCommandSource
import org.incendo.cloud.suggestion.Suggestion
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class JLineTabCompleter(private val commandProvider: CommandProvider) : Completer {
    override fun complete(
        lineReader: LineReader,
        parsedLine: ParsedLine,
        p2: MutableList<Candidate>,
    ) {
        val line = parsedLine.line()
        val suggestions: List<String> =
            commandProvider.commandManager
                .suggestionFactory()
                .suggest(ConsoleCommandSource(), line)
                .join()
                .list()
                .stream()
                .map(Suggestion::suggestion)
                .toList()

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
