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