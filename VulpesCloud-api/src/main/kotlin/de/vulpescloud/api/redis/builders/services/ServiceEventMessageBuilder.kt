package de.vulpescloud.api.redis.builders.services

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates


object ServiceEventMessageBuilder {

    fun stateEventBuilder(): StateEventBuilder {
        return StateEventBuilder()
    }
    fun consoleEventBuilder(): ConsoleEventBuilder {
        return ConsoleEventBuilder()
    }

    class ConsoleEventBuilder() {
        private var service: Service? = null
        private var line: String? = null

        fun setService(service: Service): ConsoleEventBuilder {
            this.service = service
            return this
        }

        fun setLine(line: String): ConsoleEventBuilder {
            this.line = line
            return this
        }

        fun build(): String {
            if (line == null) {
                throw NullPointerException("The line is null!")
            }
            if (service == null) {
                throw NullPointerException("The service is null!")
            }
            return "SERVICE\uD835\uDF06%name%\uD835\uDF06EVENT\uD835\uDF06LOG\uD835\uDF06%line%"
                .replace("%name%", this.service!!.name())
                .replace("%line%", line!!)
        }
    }

    class StateEventBuilder() {

        private var serviceName: String? = null
        private var serviceState: ServiceStates? = null

        fun setService(service: Service): StateEventBuilder {
            serviceName = service.name()
            return this
        }

        fun setState(state: ServiceStates): StateEventBuilder {
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