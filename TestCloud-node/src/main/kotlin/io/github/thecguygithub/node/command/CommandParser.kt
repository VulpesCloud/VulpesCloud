package io.github.thecguygithub.node.command

import io.github.thecguygithub.node.command.type.KeywordArgument
import io.github.thecguygithub.node.command.type.StringArrayArgument
import io.github.thecguygithub.node.logging.Logger


object CommandParser {

    fun serializer(commandService: CommandService, name: String, args: Array<String?>?) {
        // all commands with the same start name
        val commands = commandService.commandsByName(name)

        if (executeCommand(commands!!, args)) {
            return
        }



        for (command in commands) {

            if (command!!.defaultExecution != null) {
                // command!!.defaultExecution().execute(CommandContext())
                command.defaultExecution!!.execute(CommandContext())
            } else {
                for (syntaxCommand in command.commandSyntaxes) {
                    Logger().info(command.name + syntaxCommand.usage())
                }
            }

        }

    }

    private fun executeCommand(commands: List<Command?>, args: Array<String?>?): Boolean {
        for (command in commands) {
            if (!command!!.hasSyntaxCommands() || args!!.isEmpty()) {
                return false
            }

            for (syntaxCommand in command.commandSyntaxes) {
                if (args.size != syntaxCommand.arguments.size &&
                    syntaxCommand.arguments.none { it is StringArrayArgument }
                ) {
                    continue
                }

                val commandContext = CommandContext()
                var provedSyntax = true
                var provedSyntaxWarning: String? = null

                for (i in syntaxCommand.arguments.indices) {
                    val argument = syntaxCommand.arguments[i]

                    if (i >= args.size) {
                        provedSyntax = false
                        break
                    }

                    val rawInput = args[i]!!

                    when (argument) {
                        is StringArrayArgument -> {
                            commandContext.append(argument, argument.buildResult(args.copyOfRange(i, args.size).joinToString(" ")))
                            break
                        }
                        is KeywordArgument -> {
                            if (!argument.key.equals(rawInput, ignoreCase = true)) {
                                provedSyntax = false
                                break
                            }
                        }
                        else -> {
                            if (!argument.predication(rawInput!!)) {
                                provedSyntaxWarning = argument.wrongReason()
                                continue
                            }
                        }
                    }

                    argument.buildResult(rawInput)?.let { commandContext.append(argument, it) }
                }

                if (!provedSyntax) continue

                provedSyntaxWarning?.let {
                    Logger().warn(it)
                    return true
                }

                syntaxCommand.execution.execute(commandContext)
                return true
            }
        }
        return false
    }
}