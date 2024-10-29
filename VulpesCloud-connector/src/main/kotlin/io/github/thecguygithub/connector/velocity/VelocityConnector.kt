package io.github.thecguygithub.connector.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.plugin.Plugin
import org.slf4j.Logger


@Plugin(id = "vulpescloud", name = "VulpesCloud-Connector", authors = ["TheCGuy"])
@Suppress("unused")
class VelocityConnector @Inject constructor(
    val logger: Logger,
    val eventManager: EventManager
){

    @Subscribe(order = PostOrder.FIRST)
    fun start() {

    }

}