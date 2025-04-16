package de.vulpescloud.node.event

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.event.EventListener
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.RegisteredListener
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

    override fun call(event: Event) {
        val allHandlers = listeners.values.flatten().sortedBy { it.order }

        for (registered in allHandlers) {
            registered.handler(event)
        }
    }

    override fun unregisterListener(listener: Any): EventManager {
        listeners.remove(listener)

        return this
    }
}
