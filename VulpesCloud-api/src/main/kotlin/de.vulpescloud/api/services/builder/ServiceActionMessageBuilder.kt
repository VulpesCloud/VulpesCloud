package de.vulpescloud.api.services.builder

import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.action.ServiceActions

class ServiceActionMessageBuilder {

    companion object {
        private var action: ServiceActions? = null
        private var parameter: String = ""
        private var service: ClusterService? = null


        fun setAction(action: ServiceActions): Companion {
            this.action = action
            return this
        }

        fun setService(service: ClusterService): Companion {
            this.service = service
            return this
        }

        fun setParameter(parameter: String): Companion {
            this.parameter = parameter
            return this
        }

        fun build(): String {
            if (service == null) {
                throw NullPointerException()
            }
            if (action == null) {
                throw NullPointerException()
            }
            return "SERVICE;%name%;ACTION;%action%;%parameter%"
                .replace("%name%", service!!.name())
                .replace("%action%", action!!.name)
                .replace("%parameter%", parameter)
        }
    }

}