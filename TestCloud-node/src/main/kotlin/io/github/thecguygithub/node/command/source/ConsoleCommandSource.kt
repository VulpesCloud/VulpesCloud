package io.github.thecguygithub.node.command.source

import io.github.thecguygithub.node.logging.Logger
import lombok.NonNull

class ConsoleCommandSource : CommandSource {
    /**
     * {@inheritDoc}
     */
    @NonNull
    override fun name(): String {
        return "Console"
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(@NonNull message: String?) {
        LOGGER.info(message!!)
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(vararg messages: String?) {
        for (message in messages) {
            LOGGER.info(message!!)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(@NonNull messages: Collection<String?>?) {
        for (message in messages!!) {
            LOGGER.info(message!!)
        }
    }

    /**
     * @param permission the permission to check for
     * @return always true as the console is allowed to execute every command
     * @throws NullPointerException if permission is null.
     */
    override fun checkPermission(@NonNull permission: String?): Boolean {
        return true
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    override fun toString(): String {
        return this.name()
    }

    companion object {
        val INSTANCE: ConsoleCommandSource = ConsoleCommandSource()
        private val LOGGER = Logger()
    }
}