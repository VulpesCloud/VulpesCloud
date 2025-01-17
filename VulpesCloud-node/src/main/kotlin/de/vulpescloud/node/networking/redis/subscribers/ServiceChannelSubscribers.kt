package de.vulpescloud.node.networking.redis.subscribers

import de.vulpescloud.api.language.Translator
import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.node.Node
import de.vulpescloud.node.networking.redis.RedisJsonParser.convert
import de.vulpescloud.node.networking.redis.RedisManager
import org.slf4j.LoggerFactory

class ServiceChannelSubscribers {

    private val channels = listOf(
        RedisChannelNames.VULPESCLOUD_SERVICE_EVENT.name
    )
    private val redis = Node.instance.getRC()
    private val redisManager = RedisManager(redis!!.getJedisPool())
    private val logger = LoggerFactory.getLogger(ServiceChannelSubscribers::class.java)

    init {
        redisManager.subscribe(channels) { _, channel, message ->
            when (channel) {
                RedisChannelNames.VULPESCLOUD_SERVICE_EVENT.name -> {
                    val msg = convert(message!!)

                    val service = Node.instance.serviceProvider.findServiceByName(msg.getString("serviceName"))

                    when (msg.getString("serviceState")) {
                        ServiceStates.PREPARED.name -> { logger.info(Translator.trans("node.service.event.state.prepared"), service!!.name()) }
                        ServiceStates.CONNECTING.name -> { logger.info(Translator.trans("node.service.event.state.connecting"), service!!.name()) }
                        ServiceStates.ONLINE.name -> { logger.info(Translator.trans("node.service.event.state.started"), service!!.name()) }
                        ServiceStates.STOPPING.name -> { logger.info(Translator.trans("node.service.event.state.stopped"), service!!.name()) }
                    }
                }
            }
        }
    }

}