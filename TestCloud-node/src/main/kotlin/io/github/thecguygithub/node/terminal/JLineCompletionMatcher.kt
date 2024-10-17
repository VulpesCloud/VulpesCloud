package io.github.thecguygithub.node.terminal

import com.google.common.base.Strings
import lombok.NonNull
import org.jetbrains.annotations.Nullable
import org.jline.reader.Candidate
import org.jline.reader.CompletingParsedLine
import org.jline.reader.CompletionMatcher
import org.jline.reader.LineReader
import java.util.*
import kotlin.concurrent.Volatile


internal class JLineCompletionMatcher : CompletionMatcher {
    @Volatile
    private var candidates: List<Candidate>? = null

    @Volatile
    private var parsedLine: CompletingParsedLine? = null

    override fun compile(
        @NonNull options: Map<LineReader.Option?, Boolean?>?,
        prefix: Boolean,
        @NonNull line: CompletingParsedLine?,
        caseInsensitive: Boolean,
        errors: Int,
        @Nullable originalGroupName: String?,
    ) {
        this.candidates = null
        this.parsedLine = line
    }

    @NonNull
    override fun matches(@NonNull candidates: List<Candidate>?): List<Candidate>? {
        this.candidates = candidates
        return candidates
    }

    @Nullable
    override fun exactMatch(): Candidate? {
        // keep a local copy of the variables in case of concurrent calls
        val candidates: List<Candidate>? = Objects.requireNonNull(this.candidates)
        val parsedLine: CompletingParsedLine? = Objects.requireNonNull(this.parsedLine)

        // check if there is a 100% match
        val givenWord = parsedLine?.word()
        if (candidates != null) {
            for (candidate in candidates) {
                if (candidate.complete() && givenWord.equals(candidate.value(), true)) {
                    return candidate
                }
            }
        }

        // no exact match
        return null
    }

    override fun getCommonPrefix(): String? {
        // keep a local copy of the candidates in case of concurrent calls
        val candidates = Objects.requireNonNull(this.candidates)

        var commonPrefix: String? = null
        if (candidates != null) {
            for (candidate in candidates) {
                if (candidate.complete()) {
                    commonPrefix = if (commonPrefix == null) {
                        // no common prefix yet
                        candidate.value()
                    } else {
                        // get the common prefix
                        Strings.commonPrefix(commonPrefix, candidate.value())
                    }
                }
            }
        }
        return commonPrefix
    }
}