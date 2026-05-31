package de.vulpescloud.node.tasks

import build.buf.gen.vulpescloud.tasks.v1.*
import com.google.protobuf.Timestamp
import de.vulpescloud.api.services.Service
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.NodeCoroutineScope
import de.vulpescloud.node.grpc.GrpcContextKeys
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import de.vulpescloud.node.services.AbstractService
import de.vulpescloud.node.utils.MongoUtils
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory

class TasksAPIService : TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger("TasksAPIService")
    private val tasksDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("tasks")
    }

    @RequiresPermission("tasks.create")
    override suspend fun createTask(request: CreateTaskRequest): CreateTaskResponse {
        val task = Task.fromDefinition(request.task)

        val hasTask = tasksDatabase.get(task.name) != null

        if (hasTask) {
            logger.info("Task with name ${task.name} already exists!")
            return CreateTaskResponse.newBuilder().build()
        }

        logger.info("Creating task ${task.name}...")
        tasksDatabase.upsert(task.name, Json.encodeToJsonElement(task))

        return CreateTaskResponse.newBuilder().setTask(request.task).build()
    }

    @RequiresPermission("tasks.delete")
    override suspend fun deleteTask(request: DeleteTaskRequest): DeleteTaskResponse {
        val task = Task.fromDefinition(request.task)

        val hasTask = tasksDatabase.get(task.name) != null

        if (!hasTask) {
            logger.info("Task with name ${task.name} does not exist!")
            return DeleteTaskResponse.newBuilder().setTask(task.toDefinition()).build()
        }
        logger.info("Deleting task ${task.name}...")
        tasksDatabase.delete(task.name)

        return DeleteTaskResponse.newBuilder().setTask(task.toDefinition()).build()
    }

    @RequiresPermission("tasks.getAll")
    override suspend fun getAllTasks(request: GetAllTasksRequest): GetAllTasksResponse {
        val tasks = tasksDatabase.getAll().map { Json.decodeFromJsonElement(Task.serializer(), it) }

        return GetAllTasksResponse.newBuilder().addAllTasks(tasks.map { it.toDefinition() }).build()
    }

    @RequiresPermission("tasks.get")
    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {
        val task =
            Json.decodeFromJsonElement(
                Task.serializer(),
                tasksDatabase.get(request.name) ?: return GetByNameResponse.newBuilder().build(),
            )

        return GetByNameResponse.newBuilder().setTask(task.toDefinition()).build()
    }

    @RequiresPermission("tasks.update")
    override suspend fun updateTask(request: UpdateTaskRequest): UpdateTaskResponse {
        MongoUtils.updateTask(Task.fromDefinition(request.task))
        return UpdateTaskResponse.newBuilder().setTask(request.task).build()
    }

    @RequiresPermission("tasks.prepareServiceOnTask")
    override suspend fun prepareServiceOnTask(
        request: PrepareServiceOnTaskRequest
    ): PrepareServiceOnTaskResponse {
        if (request.nodeName != Node.instance.configProvider.config.nodeName) {
            logger.error(
                "Got request to prepare service on task ${request.task.name} for Node ${request.nodeName}!"
            )
            return PrepareServiceOnTaskResponse.newBuilder().build()
        }
        val services = mutableListOf<AbstractService>()

        logger.info("Preparing ${request.amount} service(s) of task ${request.task.name}...")
        for (i in 1..request.amount) {
            logger.info("Preparing Service ${request.task.name}-$i...")
            val factory =
                Node.instance.serviceFactoryProvider.findServiceFactory(
                    request.task.serviceFactoryName
                )

            if (factory == null) {
                logger.error("Unable to find ServiceFactory ${request.task.serviceFactoryName}")
                continue
            }
            services.add(
                factory.prepareService(
                    Service(
                        Task.fromDefinition(request.task),
                        UUID.randomUUID(),
                        factory.findNextAvailableOrderedId(
                            Task.fromDefinition(request.task),
                            request.startId,
                        ),
                        factory.detectServicePort(Task.fromDefinition(request.task)),
                        request.nodeName,
                        0,
                        Timestamp.newBuilder().build(),
                        ServiceStates.UNKNOWN,
                        Node.instance.configProvider.config.serviceBindAdress,
                    )
                )
            )
        }

        logger.info("Prepared ${request.amount} service(s) of task ${request.task.name}!")
        if (request.start) {
            NodeCoroutineScope.launch {
                logger.info("Starting ${request.amount} service(s) of task ${request.task.name}...")
                services.forEach {
                    it.start()
                    delay(
                        Node.instance.configProvider.config.serviceStartDelayMillis.toDuration(
                            DurationUnit.MILLISECONDS
                        )
                    )
                }
                logger.info("Started ${request.amount} service(s) of task ${request.task.name}!")
            }
        }

        return PrepareServiceOnTaskResponse.newBuilder()
            .addAllServices(services.map { it.service.toDefinition() })
            .build()
    }
}
