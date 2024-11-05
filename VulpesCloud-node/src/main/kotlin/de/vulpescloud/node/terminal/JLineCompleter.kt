package de.vulpescloud.node.terminal

import lombok.NonNull
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class JLineCompleter : Completer {
    private var console: JLineTerminal? = null

    fun JLine3Completer(@NonNull console: JLineTerminal?) {
        this.console = console
    }

    override fun complete(
        @NonNull reader: LineReader?,
        @NonNull line: ParsedLine,
        @NonNull candidates: MutableList<Candidate?>,
    ) {
        // iterate over all enabled tab complete handlers and record their completions
        // make sure to pass a sort to the candidate in order to keep the order the same way as given
        var currentCandidateSort = 1
        for (completeHandler in console?.tabCompleteHandlers()?.values!!) {
            if (completeHandler.enabled()) {
                // compute the completions of the handler and add the candidates
                val completions = completeHandler.completeInput(line.line())
                if (completions != null) {
                    for (completion in completions) {
                        val candidate: Candidate =
                            Candidate(completion, completion, null, null, null, null, true, currentCandidateSort++)
                        candidates.add(candidate)
                    }
                }
            }
        }
    }
}