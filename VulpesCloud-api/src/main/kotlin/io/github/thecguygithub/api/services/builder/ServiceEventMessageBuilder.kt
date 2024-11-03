package io.github.thecguygithub.api.services.builder

import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.api.services.ClusterServiceStates

class ServiceEventMessageBuilder {

    companion object {
        fun stateEventBuilder(): StateEventBuilder {
            return StateEventBuilder()
        }
    }

    class StateEventBuilder() {

        private var serviceName: String? = null
        private var serviceState: ClusterServiceStates? = null

        fun setService(service: ClusterService): StateEventBuilder {
            serviceName = service.name()
            return this
        }

        fun setState(state: ClusterServiceStates): StateEventBuilder {
            serviceState = state
            return this
        }

        fun build(): String {
            if (serviceState == null) {
                throw NullPointerException("The serviceName is null!")
            }
            if (serviceName == null) {
                throw NullPointerException("The serviceName is null!")
            }
            return "SERVICE;%name%;EVENT;STATE;%state%"
                .replace("%name%", serviceName!!)
                .replace("%state%", serviceState!!.name)
        }
    }
}