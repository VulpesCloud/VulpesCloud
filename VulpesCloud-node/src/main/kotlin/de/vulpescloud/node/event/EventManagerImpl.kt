package de.vulpescloud.node.event

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.RegisteredListener
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject
import org.slf4j.LoggerFactory
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmName

class EventManagerImpl : EventManager {

    private val listeners = mutableMapOf<Any, List<RegisteredListener>>()
    private val logger = LoggerFactory.getLogger(EventManagerImpl::class.java)

    override fun registerListener(listener: Any): EventManager {
        val registered =
            listener::class
                .memberFunctions
                .filter { it.hasAnnotation<EventListener>() && it.parameters.size == 2 }
                .map { function ->
                    val annotation = function.findAnnotation<EventListener>()!!
                    val paramType = function.parameters[1].type

                    RegisteredListener(
                        listener,
                        { event ->
                            if (paramType.isSupertypeOf(event::class.createType())) {
                                function.call(listener, event)
                            }
                        },
                        annotation.order,
                    )
                }

        if (registered.isNotEmpty()) {
            listeners[listener] = registered
        }

        logger.debug("Registered Listeners for class ${listener::class.jvmName}")

        return this
    }

    override fun callLocal(event: Event) {
        val allHandlers = listeners.values.flatten().sortedBy { it.order }

        for (registered in allHandlers) {
            registered.handler(event)
        }
    }

    fun callGlobal(event: Event, channel: RedisChannels) {
        getRC()?.sendMessage(
            JSONObject()
                .put("type", "EVENT")
                .put("eventData", JSONObject(event))
                .toString(),
            channel.name
        )
    }

    override fun unregisterListener(listener: Any): EventManager {
        listeners.remove(listener)

        return this
    }
}
