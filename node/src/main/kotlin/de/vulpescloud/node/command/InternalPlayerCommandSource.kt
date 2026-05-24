package de.vulpescloud.node.command

import de.vulpescloud.node.grpc.security.model.UserModel
import java.util.concurrent.ConcurrentHashMap

class InternalPlayerCommandSource(val user: UserModel) : CommandSource {
    val messages: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun sendMessage(message: String) {
        messages.add(message)
    }


}
