package io.github.thecguygithub.node.command.type

import io.github.thecguygithub.node.command.CommandArgument
import io.github.thecguygithub.node.command.CommandContext
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Unmodifiable


class IntArgument(key: String?) : CommandArgument<Int?>() {
    override fun predication(rawInput: String): Boolean {
        try {
            rawInput.toInt()
            return true
        } catch (e: NumberFormatException) {
            return false
        }
    }

    @Contract(pure = true)
    override fun wrongReason(): String {
        return ("The argument $key").toString() + " is not a number!"
    }

    @Contract(pure = true)
    override fun defaultArgs(context: CommandContext?): List<String> {
        return listOf<String>()
    }

    @Contract(pure = true)
    override fun buildResult(input: String?): Int {
        return input!!.toInt()
    }
}