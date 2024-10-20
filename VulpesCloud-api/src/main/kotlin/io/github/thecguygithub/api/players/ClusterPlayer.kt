package io.github.thecguygithub.api.players

import io.github.thecguygithub.api.Named
import io.github.thecguygithub.api.services.ClusterService
import lombok.SneakyThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


interface ClusterPlayer : Named {

    fun uniqueId(): UUID

    fun currentProxyName(): String

    fun currentServerName(): String

    fun currentProxyAsync(): CompletableFuture<ClusterService?>

    fun currentServerAsync(): CompletableFuture<ClusterService?>

    fun sendTitle(title: String, subtitle: String?, fadeIn: Int, stay: Int, fadeOut: Int)

    fun sendTablist(header: String, footer: String)

    fun sendMessage(message: String)

    fun sendActionBar(message: String)

    fun connect(serviceId: String)

    @SneakyThrows
    fun currentProxy(): ClusterService? {
        return currentProxyAsync()[5, TimeUnit.SECONDS]
    }

    @SneakyThrows
    fun currentServer(): ClusterService? {
        return currentServerAsync()[5, TimeUnit.SECONDS]
    }

    fun details(): String {
        return "uniqueId&8=&7" + uniqueId() + "&8, &7current proxy&8=&7" + currentProxyName() + ", &7 current server&8=&7" + currentServerName()
    }

    fun sendTitle(title: String, subtitle: String?) {
        //default minecraft values
        this.sendTitle(title, subtitle, 10, 70, 20)
    }

    fun connect(service: ClusterService) {
        this.connect(service.name())
    }

}