package de.vulpescloud.bridge.task

import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.api.tasks.Task
import de.vulpescloud.bridge.json.TaskSerializer.taskFromJson
import de.vulpescloud.wrapper.Wrapper
import org.json.JSONObject

object TaskProvider {

    fun tasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        val list = Wrapper.instance.getRC()?.getAllHashValues(RedisHashNames.VULPESCLOUD_TASKS.name)
        if (!list.isNullOrEmpty()) {
            list.forEach {
                val task = taskFromJson(JSONObject(it))
                taskList.add(task)
            }
        }
        return taskList
    }

}