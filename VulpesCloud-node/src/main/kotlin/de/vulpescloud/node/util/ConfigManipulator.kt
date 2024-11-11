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