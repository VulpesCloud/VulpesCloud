package de.vulpescloud.api.event

interface EventManager {

    fun registerListener(listener: Any): EventManager

    fun call(event: Event)

    fun unregisterListener(listener: Any): EventManager

}
