package de.vulpescloud.node.tasks

import de.vulpescloud.api.redis.RedisChannelNames
import de.vulpescloud.api.redis.RedisHashNames
import de.vulpescloud.node.Node
import de.vulpescloud.node.json.TaskSerializer.taskFromJson
import org.json.JSONObject

object TaskFactory {

    fun createTask(taskJson: JSONObject) {
        val task = taskFromJson(taskJson)
        Node.instance.getRC()?.setHashField(RedisHashNames.VULPESCLOUD_TASKS.name, task.name(), taskJson.toString())
        Node.instance.getRC()?.sendMessage("TASK;CREATE;NAME;${task.name()}", RedisChannelNames.VULPESCLOUD_TASK_CREATE.name)

    }

}