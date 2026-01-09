package de.vulpescloud.node.modules

interface VulpesModule {

    fun onLoad()

    fun onUnload()

    fun onEnable()

    fun onDisable()

}