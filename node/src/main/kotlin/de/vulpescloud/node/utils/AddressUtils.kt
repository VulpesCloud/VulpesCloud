package de.vulpescloud.node.utils

import java.net.NetworkInterface

object AddressUtils {

    fun getAvailableAddresses(): List<String> {
        return NetworkInterface.getNetworkInterfaces()
            .toList()
            .flatMap { it.inetAddresses.toList() }
            .map { it.hostAddress?.substringBefore('%') ?: "" }
            .filter { it.isNotEmpty() }
    }

}
