package de.vulpescloud.api.task

interface TaskProvider {

    fun getTaskByName(name: String): Task?

    fun tasks(): List<Task>

    fun updateTask(task: Task, updateInMySQL: Boolean = true)

}
