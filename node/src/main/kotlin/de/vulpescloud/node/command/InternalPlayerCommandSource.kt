package de.vulpescloud.node.command

import de.vulpescloud.node.grpc.security.model.UserModel
import java.util.concurrent.ConcurrentLinkedQueue

class InternalPlayerCommandSource(val user: UserModel) : CommandSource {
    val messages = ConcurrentLinkedQueue<String>()

    override fun sendMessage(message: String) {
        messages.add(message)
    }

    override fun sendError(message: String) {
        messages.add(message)
    }
}
