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