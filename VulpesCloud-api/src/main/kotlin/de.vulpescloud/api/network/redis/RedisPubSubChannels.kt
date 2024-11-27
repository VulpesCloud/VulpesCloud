package de.vulpescloud.api.network.redis

/**
 * This enum class contains all different redis channel names, that are used by VulpesCloud.
 */
enum class RedisPubSubChannels {

    VULPESCLOUD_TASK_CREATE,
    VULPESCLOUD_SERVICE_ACTION,
    VULPESCLOUD_SERVICE_EVENT,
    VULPESCLOUD_SERVICE_AUTH,
    VULPESCLOUD_SERVICE_REGISTER,
    VULPESCLOUD_SERVICE_UNREGISTER,
    VULPESCLOUD_PLAYER_ACTION,
    VULPESCLOUD_PLAYER_EVENT;

}