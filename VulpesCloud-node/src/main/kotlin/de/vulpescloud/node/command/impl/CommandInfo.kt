package de.vulpescloud.node.command.impl

import java.util.*

data class CommandInfo(
    val name: String,
    val aliases: Set<String>,
    val description: String,
    val usage: List<String>,
    val permission: String
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
}