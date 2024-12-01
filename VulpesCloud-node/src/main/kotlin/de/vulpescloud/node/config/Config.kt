package de.vulpescloud.node.config

import java.util.UUID

data class Config(
    var uuid: UUID,
    var name: String,
    var mysql: MySQLEndpointData,
    var redis: RedisEndpointData,
    var ranFirstSetup: Boolean,
)
