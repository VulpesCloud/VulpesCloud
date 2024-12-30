package de.vulpescloud.node.manager

import de.vulpescloud.api.language.Translator
import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.redis.builders.NodeAuthMessageBuilder
import de.vulpescloud.api.utils.StringUtils
import de.vulpescloud.node.Node
import de.vulpescloud.node.networking.redis.RedisJsonParser.convert
import de.vulpescloud.node.networking.redis.RedisManager
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*

class AuthenticationManager {

    private var token: String? = null

    private var channels = mutableListOf(RedisChannelNames.VULPESCLOUD_NODE_AUTH.name, RedisChannelNames.VULPESCLOUD_SERVICE_AUTH.name)
    private val logger = LoggerFactory.getLogger(AuthenticationManager::class.java)

    fun sendAuthentication() {
        Node.instance.getRC()?.sendMessage(
            NodeAuthMessageBuilder
                .setSecret(token!!)
                .setNodeName(Node.instance.config.name)
                .build(),
            RedisChannelNames.VULPESCLOUD_NODE_AUTH.name
        )
    }

    fun getAuthToken(): String {
        return if (token != null) {
            token!!
        } else {
            throw NullPointerException("")
        }
    }

    fun initializeAuth() {
        val authFile = Path.of("node/auth.secret")
        Path.of("node/").toFile().mkdirs()
        if (authFile.toFile().exists()) {
            token = authFile.toFile().readText()
        } else {
            authFile.toFile().writeText(StringUtils.generateRandomString(16))
            token = authFile.toFile().readText()
        }

        val redisManger = Node.instance.getRC()?.let { RedisManager(it.getJedisPool()) }

        redisManger?.subscribe(channels) { _, channel, message ->
            when (channel) {
                RedisChannelNames.VULPESCLOUD_NODE_AUTH.name -> {
                    val msg = convert(message!!)
                    logger.info(Translator.trans("node.authenticated"), msg.get("nodeName"))
                }
                RedisChannelNames.VULPESCLOUD_SERVICE_AUTH.name -> {
                    val msg = convert(message!!)

                    val serviceByName = Node.instance.serviceProvider.findServiceByName(msg.getString("serviceName"))
                    val serviceById = Node.instance.serviceProvider.findServiceById(UUID.fromString(msg.getString("serviceId")))

                    if (serviceById == serviceByName) {
                        logger.info(Translator.trans("node.service.authenticated"), msg.get("serviceName"))
                    } else {
                        logger.warn(Translator.trans("node.service.authenticated.failure"), msg.get("serviceName"), msg.get("serviceId"))
                    }
                }
            }
        }

    }

}