package io.github.thecguygithub.node.command.type

import io.github.thecguygithub.node.command.CommandArgument
import io.github.thecguygithub.node.command.CommandContext

class EnumArgument<E : Enum<E>>(
    private val enumClass: Class<E>?,
    key: String
) : CommandArgument<E>() {

    override fun defaultArgs(context: CommandContext?): List<String?> {
        return enumClass!!.enumConstants.map { it.name.lowercase() }
    }

    override fun predication(rawInput: String): Boolean {
        return enumClass!!.enumConstants.any { it.name.equals(rawInput, ignoreCase = true) }
    }

    override fun buildResult(input: String?): E {
        return enumClass!!.enumConstants.first { it.name == input!!.uppercase() }
    }
}