package de.vulpescloud.bridge.impl.tasks

import build.buf.gen.vulpescloud.tasks.v1.*
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.bridge.TasksAPI
import de.vulpescloud.wrapper.Wrapper

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
