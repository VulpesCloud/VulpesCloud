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

package org.vulpesstudios.vulpescloud.node.tasks

import build.buf.gen.vulpescloud.tasks.v1.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.node.Node
import org.vulpesstudios.vulpescloud.node.grpc.security.annotations.RequiresPermission
import org.vulpesstudios.vulpescloud.node.utils.MongoUtils

class TasksAPIService : TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger("TasksAPIService")
    private val tasksDatabase by lazy {
        Node.instance.getDatabaseProvider().getOrCreateDatabase("tasks")
    }

    @RequiresPermission("tasks.create", ["task=task.name"])
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

    @RequiresPermission("tasks.delete", ["task=task.name"])
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

    @RequiresPermission("tasks.get", ["task=name"])
    override suspend fun getByName(request: GetByNameRequest): GetByNameResponse {
        val task =
            Json.decodeFromJsonElement(
                Task.serializer(),
                tasksDatabase.get(request.name) ?: return GetByNameResponse.newBuilder().build(),
            )

        return GetByNameResponse.newBuilder().setTask(task.toDefinition()).build()
    }

    @RequiresPermission("tasks.update", ["task=task.name"])
    override suspend fun updateTask(request: UpdateTaskRequest): UpdateTaskResponse {
        MongoUtils.updateTask(Task.fromDefinition(request.task))
        return UpdateTaskResponse.newBuilder().setTask(request.task).build()
    }
}
