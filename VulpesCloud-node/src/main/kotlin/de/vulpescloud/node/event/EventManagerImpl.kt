package de.vulpescloud.node.event

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.annotations.EventHandler
import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.node.Node
import org.json.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure

class EventManagerImpl : EventManager {
    private val listeners = mutableMapOf<KClass<*>, MutableList<(Any) -> Unit>>()

    override fun <T : Event> listen(type: KClass<T>, listener: (T) -> Unit) {
        listeners.computeIfAbsent(type) { mutableListOf() }.add { event ->
            @Suppress("UNCHECKED_CAST")
            listener(event as T)
        }
    }

    inline fun <reified T : Event> listen(noinline listener: (T) -> Unit) {
        listen(T::class, listener)
    }

    override fun call(event: Event) {
        val json = JSONObject()
            .put("eventName", event.name())
            .put("eventData", JSONObject(event))
        Node.instance.getRC()?.sendMessage(json.toString(), RedisChannelNames.VULPESCLOUD_EVENTS.name)
        val eventType = event::class
        listeners[eventType]?.forEach { it.invoke(event) }
    }

    override fun registerListener(listener: Any) {
        val methods = listener::class.memberFunctions
        for (method in methods) {
            val annotation = method.findAnnotation<EventHandler>()
            if (annotation != null) {
                val parameters = method.parameters
                if (parameters.size == 2) { // The first parameter is always 'this'
                    val eventType = parameters[1].type.jvmErasure
                    listeners.computeIfAbsent(eventType) { mutableListOf() }.add { event ->
                        method.call(listener, event)
                    }
                }
            }
        }
    }
}