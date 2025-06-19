package de.vulpescloud.connector.common

import de.vulpescloud.api.event.events.service.ServiceStateChangeEvent
import de.vulpescloud.api.redis.RedisChannels
import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.bridge.VulpesBridge
import de.vulpescloud.bridge.VulpesBridge.getEventManager
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject

interface Connector {

    fun markOnline() {
        val oldService = VulpesBridge.getServiceProvider().getLocalService()
        oldService.state = ServiceStates.ONLINE
        getRC()
            ?.setHashField(
                "VULPESCLOUD:SERVICES",
                oldService.name,
                JSONObject(oldService).toString(),
            )
        getEventManager().callGlobal(
            ServiceStateChangeEvent(
                oldService,
                ServiceStates.STARTING,
                ServiceStates.ONLINE
            ),
            RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceStateChangeEvent
        )
    }

    fun markStopping() {
        val oldService = VulpesBridge.getServiceProvider().getLocalService()
        oldService.state = ServiceStates.STOPPING
        getRC()
            ?.setHashField(
                "VULPESCLOUD:SERVICES",
                oldService.name,
                JSONObject(oldService).toString(),
            )
        getEventManager().callGlobal(
            ServiceStateChangeEvent(
                oldService,
                ServiceStates.ONLINE,
                ServiceStates.STOPPING
            ),
            RedisChannels.VULPESCLOUD_EVENT_SERVICE_ServiceStateChangeEvent
        )
    }
}
