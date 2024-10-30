package io.github.thecguygithub.api.command

import java.util.*


@JvmRecord
data class CommandInfo(
    val name: String,
    val aliases: Set<String>,
    val description: String,
    val usage: List<String>
)  {
    /**
     * Joins the name of the registered command and the specified aliases into one String seperated by the separator.
     *
     * @param separator the separator to join with.
     * @return the joined String with the name and all aliases.
     * @throws NullPointerException if separator is null.
     */
    fun joinNameToAliases(separator: String): String {
        var result = this.name
        if (aliases.isNotEmpty()) {
            result += separator + java.lang.String.join(separator, this.aliases)
        }

        return result
    }

    /**
     * {@inheritDoc}
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CommandInfo) {
            return false
        }
        return this.name == other.name
    }

    /**
     * {@inheritDoc}
     */
    override fun hashCode(): Int {
        return Objects.hash(this.name)
    }
}