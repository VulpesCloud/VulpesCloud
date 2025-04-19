package de.vulpescloud.node.task

import de.vulpescloud.api.mysql.TaskTable
import de.vulpescloud.api.task.Task
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.utils.JsonUtils.getTask
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.json.JSONObject

class TaskProviderImpl : TaskProvider {

    override fun getTaskByName(name: String): Task? {
        return tasks().find { it.name == name }
    }

    override fun tasks(): List<Task> {
        val tasks = mutableListOf<Task>()
        getRC()?.getAllHashValues("VULPESCLOUD_TASKS")?.forEach {
            tasks.add(getTask(JSONObject(it)))
        }
        return tasks
    }

    override fun updateTask(task: Task) {
        transaction {
            val existing = TaskTable.select(TaskTable.name eq task.name).singleOrNull()

            if (existing != null) {
                TaskTable.update({ TaskTable.name eq task.name }) { it[json] = json }
            } else {
                TaskTable.insert {
                    it[name] = name
                    it[json] = json
                }
            }
        }
    }
}
