package de.vulpescloud.api.virtualconfig

import de.vulpescloud.jediswrapper.redis.ChannelListener

class VirtualConfigReloadChannelListener(val config: VirtualConfig) :
    ChannelListener("VCONFIG_RELOAD_${config.name}") {
    override fun onMessage(message: String) {
        if (message.contains("VCONFIG_RELOAD_${config.name}")) {
            config.pull()
        }
    }
}
