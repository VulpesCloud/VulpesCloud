package de.vulpescloud.bridge

import de.vulpescloud.bridge.impl.event.EventAPI
import de.vulpescloud.bridge.impl.service.ServiceCoroutineAPI
import de.vulpescloud.bridge.impl.service.ServiceFutureAPI
import de.vulpescloud.bridge.impl.tasks.TasksCoroutineAPI
import de.vulpescloud.bridge.impl.tasks.TasksFutureAPI
import de.vulpescloud.bridge.impl.virtualconfig.VirtualConfigCoroutineAPIImpl

interface BridgeAPI {

    class BridgeCoroutineAPI {

        private val tasksAPI = TasksCoroutineAPI()
        private val servicesAPI = ServiceCoroutineAPI()
        private val eventAPI = EventAPI()
        private val virtualConfigAPI = VirtualConfigCoroutineAPIImpl()

        fun getTasksAPI() = tasksAPI

        fun getServicesAPI() = servicesAPI

        fun getEventAPI() = eventAPI

        fun getVirtualConfigAPI() = virtualConfigAPI
    }

    class BridgeFutureAPI {

        private val tasksAPI = TasksFutureAPI()
        private val servicesAPI = ServiceFutureAPI()
        private val eventAPI = EventAPI()

        fun getTasksAPI() = tasksAPI

        fun getServicesAPI() = servicesAPI

        fun getEventAPI() = eventAPI
    }

    companion object {

        private val coroutineAPI = BridgeCoroutineAPI()
        private val futureAPI = BridgeFutureAPI()

        fun getCoroutineAPI(): BridgeCoroutineAPI = coroutineAPI

        fun getFutureAPI(): BridgeFutureAPI = futureAPI
    }
}
