package io.github.thecguygithub.node.command.type

import io.github.thecguygithub.node.command.CommandArgument
import io.github.thecguygithub.node.command.CommandContext
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Unmodifiable


class BooleanArgument(key: String?) : CommandArgument<Boolean?>() {
    override fun predication(rawInput: String): Boolean {
        return rawInput.equals("true", ignoreCase = true) || rawInput.equals("false", ignoreCase = true)
    }

    @Contract(pure = true)
    override fun wrongReason(): String {
        return ("The argument $key").toString() + " is not a boolean!"
    }

    @Contract(pure = true)
    override fun defaultArgs(context: CommandContext?): List<String> {
        return listOf("false", "true")
    }

    @Contract(pure = true)
    override fun buildResult(input: String?): Boolean {
        return input.toBoolean()
    }
}