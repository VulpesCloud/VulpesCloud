/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.api.services.builder

import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.ClusterServiceStates

class ServiceEventMessageBuilder {

    companion object {
        fun stateEventBuilder(): StateEventBuilder {
            return StateEventBuilder()
        }
        fun consoleEventBuilder(): ConsoleEventBuilder {
            return ConsoleEventBuilder()
        }
    }

    class ConsoleEventBuilder() {
        private var service: ClusterService? = null
        private var line: String? = null

        fun setService(service: ClusterService): ConsoleEventBuilder {
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