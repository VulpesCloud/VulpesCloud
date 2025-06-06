package de.vulpescloud.api.cluster

interface AuthenticationProvider {

    fun getAuthenticationToken(): String

}
