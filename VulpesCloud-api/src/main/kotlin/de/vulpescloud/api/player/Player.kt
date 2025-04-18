package de.vulpescloud.api.player

import java.util.UUID

data class Player(
    val name: String,
    val uuid: UUID,
    val currentProxy: String,
    val currentServer: String
)
