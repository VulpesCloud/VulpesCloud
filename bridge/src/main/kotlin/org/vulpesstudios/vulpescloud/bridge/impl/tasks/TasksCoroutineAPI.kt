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

package org.vulpesstudios.vulpescloud.bridge.impl.tasks

import build.buf.gen.vulpescloud.tasks.v1.*
import org.vulpesstudios.vulpescloud.api.tasks.Task
import org.vulpesstudios.vulpescloud.bridge.TasksAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper

class TasksCoroutineAPI : TasksAPI.TasksCoroutineAPI {

    private val tasksStub = Wrapper.instance.grpcClient.tasksAPI

    override suspend fun getTaskByName(name: String): Task? {
        val response = tasksStub.getByName(getByNameRequest { this.name = name })
        val definition = response.taskOrNull ?: return null
        return Task.fromDefinition(definition)
    }

    override suspend fun getAllTasks(): List<Task> {
        val response = tasksStub.getAllTasks(getAllTasksRequest {})
        return response.tasksList.map { Task.fromDefinition(it) }
    }

    override suspend fun deleteTask(name: String) {
        val taskDef =
            tasksStub.getByName(getByNameRequest { this.name = name }).taskOrNull ?: return
        tasksStub.deleteTask(deleteTaskRequest { this.task = taskDef })
    }

    override suspend fun deleteTask(task: Task) {
        tasksStub.deleteTask(deleteTaskRequest { this.task = task.toDefinition() })
    }

    override suspend fun createTask(task: Task) {
        tasksStub.createTask(createTaskRequest { this.task = task.toDefinition() })
    }

    override suspend fun updateTask(task: Task) {
        tasksStub.updateTask(updateTaskRequest { this.task = task.toDefinition() })
    }
}
