package de.vulpescloud.bridge.impl.tasks

import build.buf.gen.vulpescloud.tasks.v1.*
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.bridge.FutureHelper.toCompletableFuture
import de.vulpescloud.bridge.TasksAPI
import de.vulpescloud.wrapper.Wrapper
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
