/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.bridge

import org.vulpesstudios.vulpescloud.bridge.impl.event.EventAPI
import org.vulpesstudios.vulpescloud.bridge.impl.metrics.CoroutineMetricsImpl
import org.vulpesstudios.vulpescloud.bridge.impl.player.PlayerCoroutineAPIImpl
import org.vulpesstudios.vulpescloud.bridge.impl.player.PlayerFutureAPIImpl
import org.vulpesstudios.vulpescloud.bridge.impl.service.ServiceCoroutineAPI
import org.vulpesstudios.vulpescloud.bridge.impl.service.ServiceFutureAPI
import org.vulpesstudios.vulpescloud.bridge.impl.tasks.TasksCoroutineAPI
import org.vulpesstudios.vulpescloud.bridge.impl.tasks.TasksFutureAPI
import org.vulpesstudios.vulpescloud.bridge.impl.virtualconfig.VirtualConfigCoroutineAPIImpl

interface BridgeAPI {

    class BridgeCoroutineAPI {

        private val tasksAPI = TasksCoroutineAPI()
        private val servicesAPI = ServiceCoroutineAPI()
        private val eventAPI = EventAPI()
        private val virtualConfigAPI = VirtualConfigCoroutineAPIImpl()
        private val playerAPI = PlayerCoroutineAPIImpl()
        private val metricsAPI = CoroutineMetricsImpl()

        fun getTasksAPI() = tasksAPI

        fun getServicesAPI() = servicesAPI

        fun getEventAPI() = eventAPI

        fun getVirtualConfigAPI() = virtualConfigAPI

        fun getPlayerAPI() = playerAPI

        fun getMetricsAPI() = metricsAPI
    }

    class BridgeFutureAPI {

        private val tasksAPI = TasksFutureAPI()
        private val servicesAPI = ServiceFutureAPI()
        private val eventAPI = EventAPI()
        private val virtualConfigAPI = VirtualConfigCoroutineAPIImpl()
        private val playerAPI = PlayerFutureAPIImpl()

        fun getTasksAPI() = tasksAPI

        fun getServicesAPI() = servicesAPI

        fun getEventAPI() = eventAPI

        fun getCoroutineVirtualConfigAPI() = virtualConfigAPI

        fun getPlayerAPI() = playerAPI
    }

    companion object {
        fun createCoroutineAPI(): BridgeCoroutineAPI = BridgeCoroutineAPI()

        fun createFutureAPI(): BridgeFutureAPI = BridgeFutureAPI()
    }
}
