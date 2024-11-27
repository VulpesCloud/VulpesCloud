package de.vulpescloud.modules.example

import de.vulpescloud.api.module.VulpesModule
import org.slf4j.LoggerFactory

class ExampleModule : VulpesModule {
    private val logger = LoggerFactory.getLogger(ExampleModule::class.java)

    override fun enable() {
        logger.info("Enabled ExampleModule! :>")
    }

    override fun disable() {
        logger.info("Disabled ExampleModule! :<")
    }
}