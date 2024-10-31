package io.github.thecguygithub.connector.velocity

import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.github.thecguygithub.node.networking.redis.RedisManager
import java.net.InetSocketAddress

object VelocityRegistrationHandler {

    private val redis = VelocityConnector.instance.wrapper.getRC()
    private val redisManager = redis?.let { RedisManager(it.getJedisPool()) }
    private val redisChannels = listOf("vulpescloud-action-service")
    private val proxyServer = VelocityConnector.instance.proxyServer

    init {
        for (server in servers()) {
            proxyServer.getServer(server).ifPresent { proxyServer.unregisterServer(it.serverInfo) }
        }
    }

    fun registerServer(name: String, address: String, port: Int) {
        proxyServer.registerServer(ServerInfo(name, InetSocketAddress(address, port)))
    }

    fun unregisterServer(name: String) {
        proxyServer.getServer(name).ifPresent {
            proxyServer.unregisterServer(
                it.serverInfo
            )
        }
    }

    fun servers(): List<String> {
        return VelocityConnector.instance.proxyServer.allServers.stream().map { it.serverInfo.name }.toList()
    }

}