package io.github.thecguygithub.node.command.type

import io.github.thecguygithub.node.command.CommandArgument


class StringArrayArgument(key: String?) : CommandArgument<String?>() {
    override fun buildResult(input: String?): String {
        return input!!
    }
}