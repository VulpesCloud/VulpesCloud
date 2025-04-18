package de.vulpescloud.api.redis

enum class RedisChannels {

    VULPESCLOUD_EVENT_CLUSTER_NodeStateChangeEvent,
    VULPESCLOUD_EVENT_MODULE_ModuleLoadEvent,
    VULPESCLOUD_EVENT_MODULE_ModuleStartEvent,
    VULPESCLOUD_EVENT_MODULE_ModuleUnloadEvent,
    VULPESCLOUD_CLUSTER_SelectNewHeadNode;

}
