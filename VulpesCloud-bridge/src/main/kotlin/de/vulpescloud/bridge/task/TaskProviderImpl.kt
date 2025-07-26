package de.vulpescloud.bridge.task

import de.vulpescloud.api.task.Task
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.bridge.JsonUtils.getTask
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import org.json.JSONObject
import org.slf4j.LoggerFactory

class TaskProviderImpl : TaskProvider {

    private val logger = LoggerFactory.getLogger("TaskProviderImpl")

    override fun getTaskByName(name: String): Task? {
        return tasks().find { it.name == name }
    }

    override fun tasks(): List<Task> {
        try {
            val tasks = mutableListOf<Task>()
            getRC()?.getAllHashValues("VULPESCLOUD:TASKS")?.forEach {
                tasks.add(getTask(JSONObject(it)))
            }
            return tasks
        } catch (e: Exception) {
            logger.error("Error while getting tasks from Redis", e)
            return emptyList()
        }
    }

    override fun updateTask(task: Task, updateInMySQL: Boolean) {}
}
