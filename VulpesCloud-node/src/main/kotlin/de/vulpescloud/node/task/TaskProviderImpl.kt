package de.vulpescloud.node.task

import de.vulpescloud.api.mysql.TaskTable
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.utils.JsonUtils.getTask
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory

class TaskProviderImpl : TaskProvider {

    private val logger = LoggerFactory.getLogger(TaskProviderImpl::class.java)

    override fun getTaskByName(name: String): Task? {
        return tasks().find { it.name == name }
    }

    override fun tasks(): List<Task> {
        try {
            val tasks = mutableListOf<Task>()
            getRC()?.getAllHashValues("VULPESCLOUD:TASKS")?.forEach {
                try {
                    tasks.add(getTask(JSONObject(it)))
                } catch (e: JSONException) {
                    logger.error("Error while parsing task from Redis")
                }
            }
            return tasks
        } catch (e: Exception) {
            logger.error("Error while getting tasks from Redis", e)
            return emptyList()
        }
    }

    override fun updateTask(task: Task, updateInMySQL: Boolean) {
        val taskJson = JSONObject(task)
        if (updateInMySQL) {
            transaction {
                val existing = TaskTable.select(TaskTable.name eq task.name).singleOrNull()

                if (existing != null) {
                    TaskTable.update({ TaskTable.name eq task.name }) {
                        it[json] = taskJson.toString()
                    }
                } else {
                    TaskTable.insert {
                        it[name] = name
                        it[json] = taskJson.toString()
                    }
                }
            }
        }

        getRC()?.setHashField("VULPESCLOUD:TASKS", task.name, taskJson.toString())
    }
}
