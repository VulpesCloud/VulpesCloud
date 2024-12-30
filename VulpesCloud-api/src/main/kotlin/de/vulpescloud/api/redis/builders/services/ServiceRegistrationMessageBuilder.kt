package de.vulpescloud.api.redis.builders.services

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceActions
import org.json.JSONObject

object ServiceRegistrationMessageBuilder {

    fun serviceRegisterBuilder(): ServiceRegisterBuilder {
        return ServiceRegisterBuilder()
    }

    fun serviceUnregisterBuilder(): ServiceUnregisterBuilder {
        return ServiceUnregisterBuilder()
    }


    class ServiceRegisterBuilder() {
        private var service: Service? = null
        private var address: String? = null
        private var port: Int? = null

        fun setService(service: Service): ServiceRegisterBuilder {
            this.service = service
            return this
        }

        fun build(): String {
            val json = JSONObject()

            json.put("action", "SERVICE_REGISTER")
            json.put("serviceName", service!!.name())
            json.put("address", service!!.hostname())
            json.put("port", service!!.port())

            return json.toString()
        }
    }

    class ServiceUnregisterBuilder() {
        private var service: Service? = null

        fun setService(service: Service): ServiceUnregisterBuilder {
            this.service = service
            return this
        }

        fun build(): String {
            val json = JSONObject()

            json.put("action", "SERVICE_REGISTER")
            json.put("serviceName", service!!.name())

            return json.toString()
        }
    }
}