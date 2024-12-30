package de.vulpescloud.api.redis.builders.services

import de.vulpescloud.api.cluster.NodeActions
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceActions
import org.json.JSONObject

object ServiceAuthMessageBuilder {

    private var service: Service? = null
    private var secret: String? = null

    fun setService(service: Service): ServiceAuthMessageBuilder {
        this.service = service
        return this
    }

    fun setSecret(secret: String): ServiceAuthMessageBuilder {
        this.secret = secret
        return this
    }

    fun build(): String {
        val json = JSONObject()

        json.put("action", ServiceActions.AUTHORIZE.name)
        json.put("secret", secret!!)
        json.put("serviceName", service!!.name())
        json.put("serviceId", service!!.id())

        return json.toString()
    }

}