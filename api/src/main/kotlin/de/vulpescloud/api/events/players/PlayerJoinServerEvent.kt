package de.vulpescloud.api.events.players

import de.vulpescloud.api.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class PlayerJoinServerEvent(
    val playerName: String,
    @Serializable(UUIDSerializer::class) val playerUUID: UUID,
)
