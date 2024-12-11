package de.vulpescloud.node.tasks

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.node.Node
import de.vulpescloud.node.json.TaskSerializer.taskFromJson
import org.json.JSONObject

class TaskProvider {

    fun tasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        val list = Node.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_TASKS.name)
        if (!list.isNullOrEmpty()) {
            list.forEach {
                val task = taskFromJson(JSONObject(it))
                taskList.add(task)
            }
        }
        return taskList
    }

}