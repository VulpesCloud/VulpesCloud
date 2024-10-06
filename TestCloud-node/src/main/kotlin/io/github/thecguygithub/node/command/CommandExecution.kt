package io.github.thecguygithub.node.command

fun interface CommandExecution {

    fun execute(commandContext: CommandContext)

}