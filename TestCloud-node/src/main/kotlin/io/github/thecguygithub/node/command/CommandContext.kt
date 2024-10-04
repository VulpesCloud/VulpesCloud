package io.github.thecguygithub.node.command

import lombok.extern.log4j.Log4j2
import org.jetbrains.annotations.Contract


@Log4j2
class CommandContext {
    private val contexts: MutableMap<String?, Any> = HashMap()

    @Contract(pure = true)
    fun <T> arg(argument: CommandArgument<T>): T? {
        return contexts[argument.key] as T?
    }

    fun append(argument: CommandArgument<*>, value: Any) {
        contexts[argument.key] = value
    }

    fun <T> arg(id: String?): T? {
        return contexts[id] as T?
    }
}