package de.vulpescloud.node.command.impl

import de.vulpescloud.node.command.CommandSource
import org.slf4j.LoggerFactory

class ConsoleCommandSource : CommandSource {
    /**
     * {@inheritDoc}
     */
    override fun name(): String {
        return "Console"
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(message: String) {
        LOGGER.info(message)
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(vararg messages: String) {
        for (message in messages) {
            LOGGER.info(message)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(messages: Collection<String>) {
        for (message in messages) {
            LOGGER.info(message)
        }
    }

    /**
     * @param permission the permission to check for
     * @return always true as the console is allowed to execute every command
     * @throws NullPointerException if permission is null.
     */
    override fun checkPermission(permission: String): Boolean {
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun toString(): String {
        return this.name()
    }

    companion object {
        val INSTANCE: ConsoleCommandSource = ConsoleCommandSource()
        private val LOGGER = LoggerFactory.getLogger(ConsoleCommandSource::class.java)
    }
}