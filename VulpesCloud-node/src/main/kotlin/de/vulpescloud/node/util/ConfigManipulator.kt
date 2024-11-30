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

package de.vulpescloud.node.util

import java.io.*
import java.util.function.Predicate


class ConfigManipulator(private val file: File) {

    private val replacements: MutableMap<Predicate<String>, String> = HashMap()

    companion object {
        @JvmStatic
        fun of(file: File): ConfigManipulator {
            return ConfigManipulator(file)
        }
    }

    fun rewrite(predicate: Predicate<String>, line: String) {
        replacements[predicate] = line
    }

    @Throws(IOException::class)
    fun write() {
        val lines = mutableListOf<String>()

        BufferedReader(FileReader(file)).use { bufferedReader ->
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                line?.let { lines.add(it) }
            }
        }

        FileWriter(file).use { fileWriter ->
            for (line in lines) {
                var found = false
                for ((predicate, replacement) in replacements) {
                    if (predicate.test(line)) {
                        fileWriter.write("$replacement\n")
                        found = true
                        break
                    }
                }
                if (!found) {
                    fileWriter.write("$line\n")
                }
            }
        }
    }
}