package de.vulpescloud.api.tasks.builder

import de.vulpescloud.api.tasks.Task
import org.json.JSONObject

class TaskCreateMessageBuilder {

    companion object {
        private lateinit var taskData: Task

        fun setTask(task: Task): TaskCreateMessageBuilder.Companion {
            this.taskData = task
            return this
        }

        fun build(): String {
            return "TASK;CREATE;DATA;%json%"
                .replace("%json%", JSONObject(taskData).toString())
        }

    }

}