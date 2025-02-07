package de.vulpescloud.api.event

import kotlin.reflect.KClass

interface EventManager {

    fun <T : Any> listen(type: KClass<T>, listener: (T) -> Unit)

    fun call(event: Any)

    fun registerListener(listener: Any)
}