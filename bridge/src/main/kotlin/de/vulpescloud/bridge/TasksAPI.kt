package de.vulpescloud.bridge

import de.vulpescloud.api.tasks.Task
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
