package de.vulpescloud.api.services

import de.vulpescloud.api.services.builder.ServiceEventMessageBuilder
import de.vulpescloud.api.services.builder.ServiceRegistrationMessageBuilder

class ServiceMessageBuilder {

    companion object {
        fun eventMessageBuilder(): ServiceEventMessageBuilder.Companion {
            return ServiceEventMessageBuilder
        }

        fun registrationMessageBuilder(): ServiceRegistrationMessageBuilder.Companion {
            return ServiceRegistrationMessageBuilder
        }
    }

}