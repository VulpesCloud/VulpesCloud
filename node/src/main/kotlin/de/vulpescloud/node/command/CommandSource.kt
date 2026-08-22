package de.vulpescloud.node.command

import de.vulpescloud.node.grpc.security.model.UserModel

interface CommandSource {

    fun sendMessage(message: String)

    fun sendError(message: String)

    companion object {
        val CONSOLE = ConsoleCommandSource()

        fun player(user: UserModel) = InternalPlayerCommandSource(user)
    }

}
