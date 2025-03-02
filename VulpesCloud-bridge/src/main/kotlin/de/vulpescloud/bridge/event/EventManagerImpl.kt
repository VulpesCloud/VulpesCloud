package de.vulpescloud.bridge.event

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.event.annotations.EventHandler
import org.json.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure

object EventManagerImpl : EventManager {
    private val listeners = mutableMapOf<KClass<*>, MutableList<(Any) -> Unit>>()

    override fun <T : Event> listen(type: KClass<T>, listener: (T) -> Unit) {
        listeners.computeIfAbsent(type) { mutableListOf() }.add { event ->
            @Suppress("UNCHECKED_CAST")
            listener(event as T)
        }
    }

    override fun call(event: Event) {
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