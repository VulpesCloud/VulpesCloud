package io.github.thecguygithub.node.command

import io.github.thecguygithub.node.command.type.KeywordArgument


data class CommandSyntax(
    val execution: CommandExecution,
    val arguments: Array<out CommandArgument<*>>,
    var description: String? = null
) {
    fun usage(): String {
        return arguments.joinToString(" ") {
            if (it is KeywordArgument) "&f${it.key}" else "&8<&f${it.key}&8>"
        } + (description?.let { " &8- &7$it" } ?: "")
    }
}