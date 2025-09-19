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

        suspend fun prepareService(service: Service): Service?

        suspend fun prepareService(task: Task): Service?

        suspend fun startService(service: Service): Service?

        suspend fun stopService(service: Service): Service?

        suspend fun deleteService(service: Service): Service?

        suspend fun restartService(service: Service): Service?

        suspend fun sendCommand(service: Service, command: String): Boolean

        suspend fun getLocalService(): Service?
    }

    interface ServiceFutureAPI {

        fun getAllServices(): CompletableFuture<List<Service>>

        fun getServiceByName(name: String): CompletableFuture<Service?>

        fun getServiceByUUID(uuid: UUID): CompletableFuture<Service?>

        fun getServicesByTask(task: String): CompletableFuture<List<Service>>

        fun getServicesByTask(task: Task): CompletableFuture<List<Service>>

        fun prepareService(service: Service): CompletableFuture<Service?>

        fun prepareService(task: Task): CompletableFuture<Service?>

        fun startService(service: Service): CompletableFuture<Service?>

        fun stopService(service: Service): CompletableFuture<Service?>

        fun deleteService(service: Service): CompletableFuture<Service?>

        fun restartService(service: Service): CompletableFuture<Service?>

        fun sendCommand(service: Service, command: String): CompletableFuture<Boolean>

        fun getLocalService(): CompletableFuture<Service?>
    }
}
