package de.vulpescloud.node.tasks

import build.buf.gen.vulpescloud.tasks.v1.*
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.utils.MongoUtils
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.BsonString
import org.slf4j.LoggerFactory

class TasksAPIService : TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger("TasksAPIService")

    override suspend fun createTask(request: CreateTaskRequest): CreateTaskResponse {
        val task = Task.fromDefinition(request.task)
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "tasks"
                )

        val filter = BsonDocument("name", BsonString(task.name))
        val existingTaskDoc = collection.find(filter).firstOrNull()
        val existingTask = existingTaskDoc?.let { Task.fromDocument(it) }

        if (existingTask != null) {
            logger.info("Task with name ${task.name} already exists!")
            return CreateTaskResponse.newBuilder().setTask(existingTask.toDefinition()).build()
        }

        logger.info("Creating task ${task.name}...")
        collection.insertOne(task.toDocument())

        return CreateTaskResponse.newBuilder().setTask(request.task).build()
    }

    override suspend fun deleteTask(request: DeleteTaskRequest): DeleteTaskResponse {
        val task = Task.fromDefinition(request.task)
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "tasks"
                )
        val filter = BsonDocument("name", BsonString(task.name))
        val existingTaskDoc = collection.find(filter).firstOrNull()
        val existingTask = existingTaskDoc?.let { Task.fromDocument(it) }
        if (existingTask == null) {
            logger.info("Task with name ${task.name} does not exist!")
            return DeleteTaskResponse.newBuilder().setTask(task.toDefinition()).build()
        }
        logger.info("Deleting task ${task.name}...")
        collection.deleteOne(filter)

        return DeleteTaskResponse.newBuilder().setTask(task.toDefinition()).build()
    }

    override suspend fun getAllTasks(request: GetAllTasksRequest): GetAllTasksResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "tasks"
                )

        val tasks = mutableListOf<TaskDefinition>()

        collection.find().collect {
            tasks.add(Task.fromDocument(it.toBsonDocument()).toDefinition())
        }

        return GetAllTasksResponse.newBuilder().addAllTasks(tasks).build()
    }

    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {
        val collection =
            Node.instance.mongoClient
                .getDatabase(Node.instance.configProvider.config.mongodb.database)
                .getCollection<BsonDocument>(
                    Node.instance.configProvider.config.mongodb.collectionPrefix + "tasks"
                )
        val filter = BsonDocument("name", BsonString(request.name))
        val taskDoc = collection.find(filter).firstOrNull()
        val task = taskDoc?.let { Task.fromDocument(it) }
        if (task == null) {
            logger.info("Task with name ${request.name} does not exist!")
            return GetByNameResponse.newBuilder().build()
        }
        return GetByNameResponse.newBuilder().setTask(task.toDefinition()).build()
    }

    override suspend fun updateTask(request: UpdateTaskRequest): UpdateTaskResponse {
        MongoUtils.updateTask(Task.fromDefinition(request.task))
        return UpdateTaskResponse.newBuilder().setTask(request.task).build()
    }
}
