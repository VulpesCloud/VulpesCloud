package de.vulpescloud.api.services.builder

import de.vulpescloud.api.services.ClusterService

class ServiceRegistrationMessageBuilder {

    companion object {
        fun serviceRegisterBuilder(): ServiceRegisterBuilder {
            return ServiceRegisterBuilder()
        }

        fun serviceUnregisterBuilder(): ServiceUnregisterBuilder {
            return ServiceUnregisterBuilder()
        }
    }

    class ServiceRegisterBuilder() {
        private var service: ClusterService? = null
        private var address: String? = null
        private var port: Int? = null

        fun setService(service: ClusterService): ServiceRegisterBuilder {
            this.service = service
            return this
        }

        fun build(): String {
            if (service == null) {
                throw NullPointerException("The Parameter service is null")
            }

            return "SERVICE;%name%;REGISTER;ADDRESS;%address%;PORT;%port%"
                .replace("%name%", service!!.name())
                .replace("%address%", service!!.hostname()!!)
                .replace("%port%", service!!.port().toString())
        }
    }

    class ServiceUnregisterBuilder() {
        private var service: ClusterService? = null

        fun setService(service: ClusterService): ServiceUnregisterBuilder {
            this.service = service
            return this
        }

        fun build(): String {
            if (service == null) {
                throw NullPointerException("The Parameter service is null")
            }

            return "SERVICE;%name%;UNREGISTER"
                .replace("%name%", service!!.name())
        }
    }

}