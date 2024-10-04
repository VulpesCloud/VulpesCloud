package io.github.thecguygithub.node.command

import lombok.AllArgsConstructor
import lombok.Getter
import lombok.experimental.Accessors


@Getter
@Accessors(fluent = true)
@AllArgsConstructor
abstract class CommandArgument<T> {
    var key: String? = null

    open fun defaultArgs(context: CommandContext?): List<String?>? {
        return listOf<String>()
    }

    // if one argument must be special type
    open fun predication(rawInput: String): Boolean {
        return !(rawInput.startsWith("<") && rawInput.endsWith(">"))
    }

    open fun wrongReason(): String {
        return "The argument $key is not a valid parameter&8!"
    }

    abstract fun buildResult(input: String?): T
}