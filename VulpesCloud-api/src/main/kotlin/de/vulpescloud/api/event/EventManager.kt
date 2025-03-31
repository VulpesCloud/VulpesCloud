package de.vulpescloud.api.event

import kotlin.reflect.KClass

interface EventManager {

    fun <T : Event> listen(type: KClass<T>, listener: (T) -> Unit)

    fun call(event: Event)

    fun registerListener(listener: Any)
}