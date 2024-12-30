package de.vulpescloud.api.redis.builders.services

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceActions
import org.json.JSONObject

object ServiceActionMessageBuilder {

    private var action: ServiceActions? = null
    private var parameter: String = ""
    private var service: Service? = null


    fun setAction(action: ServiceActions): ServiceActionMessageBuilder {
        this.action = action
        return this
    }

    fun setService(service: Service): ServiceActionMessageBuilder {
        this.service = service
        return this
    }

    fun setParameter(parameter: String): ServiceActionMessageBuilder {
        this.parameter = parameter
        return this
    }

    fun build(): String {
        val json = JSONObject()

        json.put("action", action!!.name)
        json.put("service", service!!.name())
        json.put("parameter", parameter)

        return json.toString()
    }

}