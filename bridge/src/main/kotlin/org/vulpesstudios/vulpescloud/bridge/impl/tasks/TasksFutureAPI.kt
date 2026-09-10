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
import org.vulpesstudios.vulpescloud.bridge.FutureHelper.toCompletableFuture
import org.vulpesstudios.vulpescloud.bridge.TasksAPI
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
import java.util.concurrent.CompletableFuture

class TasksFutureAPI : TasksAPI.TasksFutureAPI {

    private val tasksStub = Wrapper.instance.grpcClient.futureTasksAPI

    override fun getTaskByName(name: String): CompletableFuture<Task?> {
        return tasksStub
            .getByName(getByNameRequest { this.name = name })
            .toCompletableFuture()
            .thenApply { response ->
                val def = response.taskOrNull ?: return@thenApply null
                Task.fromDefinition(def)
            }
    }

    override fun getAllTasks(): CompletableFuture<List<Task>> {
        return tasksStub.getAllTasks(getAllTasksRequest {}).toCompletableFuture().thenApply {
            response ->
            response.tasksList.map { Task.fromDefinition(it) }
        }
    }

    override fun deleteTask(name: String): CompletableFuture<Void> {
        return tasksStub
            .getByName(getByNameRequest { this.name = name })
            .toCompletableFuture()
            .thenCompose { response ->
                val def = response.taskOrNull
                if (def == null) {
                    CompletableFuture.completedFuture(null) // no-op
                } else {
                    tasksStub
                        .deleteTask(deleteTaskRequest { this.task = def })
                        .toCompletableFuture()
                        .thenApply { null }
                }
            }
    }

    override fun deleteTask(task: Task): CompletableFuture<Void> {
        return tasksStub
            .deleteTask(deleteTaskRequest { this.task = task.toDefinition() })
            .toCompletableFuture()
            .thenApply { null }
    }

    override fun createTask(task: Task): CompletableFuture<Void> {
        return tasksStub
            .createTask(createTaskRequest { this.task = task.toDefinition() })
            .toCompletableFuture()
            .thenApply { null }
    }

    override fun updateTask(task: Task): CompletableFuture<Void> {
        return tasksStub
            .updateTask(updateTaskRequest { this.task = task.toDefinition() })
            .toCompletableFuture()
            .thenApply { null }
    }
}
