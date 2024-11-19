package de.vulpescloud.api.network.redis

/**
 * This enum class contains all different redis channel names, that are used by VulpesCloud.
 */
enum class RedisPubSubChannels {

    VULPESCLOUD_CREATE_TASK,
    VULPESCLOUD_ACTION_SERVICE,
    VULPESCLOUD_EVENT_SERVICE,
    VULPESCLOUD_AUTH_SERVICE,
    VULPESCLOUD_REGISTER_SERVICE,
    VULPESCLOUD_UNREGISTER_SERVICE;

}