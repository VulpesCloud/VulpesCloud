package de.vulpescloud.api.event

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventListener(
    val order: EventOrder = EventOrder.NORMAL
)
