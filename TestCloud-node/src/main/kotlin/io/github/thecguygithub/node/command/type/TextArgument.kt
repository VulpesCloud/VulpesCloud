package io.github.thecguygithub.node.command.type

import io.github.thecguygithub.node.command.CommandArgument
import org.jetbrains.annotations.Contract


class TextArgument(key: String?) : CommandArgument<String?>() {
    @Contract(pure = true)
    override fun buildResult(input: String?): String {
        return input!!
    }
}