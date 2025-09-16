package de.vulpescloud.bridge

import de.vulpescloud.bridge.impl.service.ServiceCoroutineAPI
import de.vulpescloud.bridge.impl.service.ServiceFutureAPI
import de.vulpescloud.bridge.impl.tasks.TasksCoroutineAPI
import de.vulpescloud.bridge.impl.tasks.TasksFutureAPI

interface BridgeAPI {

    class BridgeCoroutineAPI {

        private val tasksAPI = TasksCoroutineAPI()
        private val servicesAPI = ServiceCoroutineAPI()

        fun getTasksAPI() = tasksAPI

        fun getServicesAPI() = servicesAPI
    }

    class BridgeFutureAPI {

        private val tasksAPI = TasksFutureAPI()
        private val servicesAPI = ServiceFutureAPI()

        fun getTasksAPI() = tasksAPI

        fun getServicesAPI() = servicesAPI
    }

    companion object {

        private val coroutineAPI = BridgeCoroutineAPI()
        private val futureAPI = BridgeFutureAPI()

        fun getCoroutineAPI(): BridgeCoroutineAPI = coroutineAPI

        fun getFutureAPI(): BridgeFutureAPI = futureAPI
    }
}
