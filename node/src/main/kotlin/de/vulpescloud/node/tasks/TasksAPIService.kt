package de.vulpescloud.node.tasks

import build.buf.gen.vulpescloud.tasks.v1.*
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.annotations.RequiresPermission
import de.vulpescloud.node.utils.MongoUtils
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
}
