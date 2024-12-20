package de.vulpescloud.api.modules

interface VulpesModule {

    fun setup() {}

    fun enable()

    fun disable()

}