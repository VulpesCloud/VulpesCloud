package io.github.thecguygithub.node.command.type

import io.github.thecguygithub.node.command.CommandArgument
import io.github.thecguygithub.node.command.CommandContext
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Unmodifiable
import java.util.List


class KeywordArgument(key: String?) : CommandArgument<String?>() {
    @Contract(" -> new")
    override fun defaultArgs(context: CommandContext?): @Unmodifiable MutableList<String> {
        return List.of(key)
    }

    @Contract(pure = true)
    override fun wrongReason(): String {
        return ""
    }

    @Contract(pure = true)
    override fun buildResult(input: String?): String {
        return ""
    }

    override fun predication(rawInput: String): Boolean {
        return super.predication(rawInput) && rawInput.equals(key, ignoreCase = true)
    }
}