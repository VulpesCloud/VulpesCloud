package de.vulpescloud.node.terminal

import de.vulpescloud.node.Node
import de.vulpescloud.node.command.source.ConsoleCommandSource
import org.incendo.cloud.suggestion.Suggestion
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class JLineCompleter : Completer {
    override fun complete(p0: LineReader, p1: ParsedLine, p2: MutableList<Candidate>) {
        val line = p1.line()
        val suggestions: List<String> = if (Node.setupProvider.currentSetup != null) {
            Node.setupProvider.getSetupAnswers(line)
        } else {
            Node.commandProvider!!.commandManager!!.suggestionFactory().suggest(ConsoleCommandSource(), line).join().list().stream()
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