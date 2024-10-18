package io.github.thecguygithub.node.command.source

import io.github.thecguygithub.api.Named
import lombok.NonNull

interface CommandSource : Named {
    /**
     * @param message the message that is sent to the source
     * @throws NullPointerException if message is null.
     */
    fun sendMessage(@NonNull message: String?)

    /**
     * @param messages the messages that are sent to the source
     * @throws NullPointerException if messages is null.
     */
    fun sendMessage(@NonNull vararg messages: String?)

    /**
     * @param messages the messages that are sent to the source
     * @throws NullPointerException if messages is null.
     */
    fun sendMessage(@NonNull messages: Collection<String?>?)

    /**
     * Used to check if the command source has the given permission
     *
     * @param permission the permission to check for
     * @return whether the source has the permission
     * @throws NullPointerException if permission is null.
     */
    fun checkPermission(@NonNull permission: String?): Boolean

    companion object {
        /**
         * @return the console command source instance
         */
        @NonNull
        fun console(): CommandSource {
            return ConsoleCommandSource.INSTANCE
        }
    }
}