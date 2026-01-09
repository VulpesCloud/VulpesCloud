package de.vulpescloud.node.command

import java.util.*

data class CommandInfo(
    val name: String,
    val aliases: Set<String>,
    val description: String,
    val usage: List<String>,
)  {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CommandInfo) {
            return false
        }
        return this.name == other.name
    }

    override fun hashCode(): Int {
        return Objects.hash(this.name)
    }

    fun joinNameToAliases(separator: String): String {
        var result = this.name
        if (aliases.isNotEmpty()) {
            result += separator + java.lang.String.join(separator, this.aliases)
        }

        return result
    }
}