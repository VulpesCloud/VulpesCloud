package de.vulpescloud.connector.common

import de.vulpescloud.api.service.ServiceStates
import de.vulpescloud.bridge.VulpesBridge
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject

interface Connector {

    fun markOnline() {
        val oldService = VulpesBridge.getServiceProvider().getLocalService()
        oldService.state = ServiceStates.ONLINE
        getRC()
            ?.setHashField(
                "VULPESCLOUD_SERVICES",
                oldService.name,
                JSONObject(oldService).toString(),
            )
    }

    fun markStopping() {
        val oldService = VulpesBridge.getServiceProvider().getLocalService()
        oldService.state = ServiceStates.STOPPING
        getRC()
            ?.setHashField(
                "VULPESCLOUD_SERVICES",
                oldService.name,
                JSONObject(oldService).toString(),
            )
    }
}
