package io.github.thecguygithub.node.command

import lombok.Getter
import lombok.experimental.Accessors
import org.jetbrains.annotations.Nullable


@Getter
@Accessors(fluent = true)
abstract class Command(val name: String, val description: String, internal vararg val aliases: String) {

    @Nullable
    var defaultExecution: CommandExecution? = null
    val commandSyntaxes: MutableList<CommandSyntax> = ArrayList<CommandSyntax>()

    fun syntax(execution: CommandExecution, vararg arguments: CommandArgument<*>) {
        commandSyntaxes.add(CommandSyntax(execution, arguments, null))
    }

    fun syntax(execution: CommandExecution, description: String, vararg arguments: CommandArgument<*>) {
        commandSyntaxes.add(CommandSyntax(execution, arguments, description))
    }

    fun defaultExecution(execution: CommandExecution?) {
        this.defaultExecution = execution
    }

    fun hasSyntaxCommands(): Boolean {
        return commandSyntaxes.isNotEmpty()
    }

    override fun equals(other: Any?): Boolean {
        return other is Command && other.name == name
    }



}