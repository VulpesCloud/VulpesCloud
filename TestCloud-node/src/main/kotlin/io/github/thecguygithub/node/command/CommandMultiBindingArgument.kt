package io.github.thecguygithub.node.command

import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Nullable


class CommandMultiBindingArgument(key: String?) : CommandArgument<Any?>() {

    // private val arguments: Array<CommandArgument<*>>

    fun CommandMultiBindingArgument(key: String?) {
        super.key
    }


    @Contract(pure = true)
    @Nullable
    override fun buildResult(input: String?): Any? {
        return null
    }
}