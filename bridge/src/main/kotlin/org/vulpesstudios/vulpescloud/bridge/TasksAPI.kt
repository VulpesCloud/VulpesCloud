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

package org.vulpesstudios.vulpescloud.bridge

import org.vulpesstudios.vulpescloud.api.tasks.Task
import java.util.concurrent.CompletableFuture

interface TasksAPI {

    interface TasksFutureAPI {
        fun getTaskByName(name: String): CompletableFuture<Task?>

        fun getAllTasks(): CompletableFuture<List<Task>>

        fun deleteTask(name: String): CompletableFuture<Void>

        fun deleteTask(task: Task): CompletableFuture<Void>

        fun createTask(task: Task): CompletableFuture<Void>

        fun updateTask(task: Task): CompletableFuture<Void>
    }

    interface TasksCoroutineAPI {
        suspend fun getTaskByName(name: String): Task?

        suspend fun getAllTasks(): List<Task>

        suspend fun deleteTask(name: String)

        suspend fun deleteTask(task: Task)

        suspend fun createTask(task: Task)

        suspend fun updateTask(task: Task)
    }
}
