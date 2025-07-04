package de.vulpescloud.api.event

data class RegisteredListener(
    val listener: Any,
    val handler: (Any) -> Unit,
    val order: EventOrder
)
