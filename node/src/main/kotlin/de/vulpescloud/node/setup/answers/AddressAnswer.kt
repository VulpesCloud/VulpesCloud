package de.vulpescloud.node.setup.answers

import de.vulpescloud.node.utils.AddressUtils
import java.net.InetAddress
import java.net.UnknownHostException

class AddressAnswer : SetupAnswer {
    override fun suggest(): List<String> {
        return AddressUtils.getAvailableAddresses()
    }

    companion object {
        fun parseAddress(value: String): Boolean {
            return try {
                InetAddress.getByName(value.substringBefore('%'))
                true
            } catch (e: UnknownHostException) {
                false
            }
        }
    }
}
