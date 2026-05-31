package de.vulpescloud.bridge

import de.vulpescloud.api.services.Service
import de.vulpescloud.api.tasks.Task
import java.util.*
import java.util.concurrent.CompletableFuture

interface ServiceAPI {

    interface ServiceCoroutineAPI {

        suspend fun getAllServices(): List<Service>

        suspend fun getServiceByName(name: String): Service?

        suspend fun getServiceByUUID(uuid: UUID): Service?

        suspend fun getServicesByTask(task: String): List<Service>

        suspend fun getServicesByTask(task: Task): List<Service>

        suspend fun startService(service: Service): String

        suspend fun stopService(service: Service): String

        suspend fun deleteService(service: Service): String

        suspend fun restartService(service: Service): String

        suspend fun sendCommand(service: Service, command: String): Boolean

        suspend fun getLocalService(): Service?
    }

    interface ServiceFutureAPI {

        fun getAllServices(): CompletableFuture<List<Service>>

        fun getServiceByName(name: String): CompletableFuture<Service?>

        fun getServiceByUUID(uuid: UUID): CompletableFuture<Service?>

        fun getServicesByTask(task: String): CompletableFuture<List<Service>>

        fun getServicesByTask(task: Task): CompletableFuture<List<Service>>

        fun startService(service: Service): CompletableFuture<String>

        fun stopService(service: Service): CompletableFuture<String>

        fun deleteService(service: Service): CompletableFuture<String>

        fun restartService(service: Service): CompletableFuture<String>

        fun sendCommand(service: Service, command: String): CompletableFuture<Boolean>

        fun getLocalService(): CompletableFuture<Service?>
    }
}
