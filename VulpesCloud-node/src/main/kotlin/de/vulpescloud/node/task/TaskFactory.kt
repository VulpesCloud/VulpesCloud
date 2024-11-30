/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.task

import de.vulpescloud.api.tasks.Task
import de.vulpescloud.api.version.VersionInfo
import de.vulpescloud.api.version.VersionType
import de.vulpescloud.node.Node
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object TaskFactory {

    private val GROUP_DIR = Path.of("local/tasks")

    private val logger = LoggerFactory.getLogger(TaskFactory::class.java)

    fun createNewTask(taskInformation: JSONObject) {

        val versionJson = taskInformation.getJSONObject("version")

        val version = VersionInfo(
            versionJson.getString("name"),
            VersionType.valueOf(versionJson.get("type").toString()),
            versionJson.getString("versions")
        )

        val nodesJson = taskInformation.getJSONArray("nodes")

        val nodes: Array<String?> = Array(nodesJson.length()) {
            if (nodesJson.isNull(it)) null else nodesJson.getString(it)
        }

        val templatesJson = taskInformation.getJSONArray("templates")

        val templates: Array<String?> = Array(templatesJson.length()) {
            if (templatesJson.isNull(it)) null else templatesJson.getString(it)
        }

        val task = TaskImpl(
            taskInformation.getString("name"),
            taskInformation.getInt("maxMemory"),
            version,
            templates.toList(),
            nodes.toList(),
            taskInformation.getInt("maxPlayers"),
            taskInformation.getBoolean("staticServices"),
            taskInformation.getInt("minOnlineCount"),
            taskInformation.getBoolean("maintenance"),
            taskInformation.getInt("startPort"),
            taskInformation.getBoolean("fallback")
        )

        GROUP_DIR.toFile().mkdirs()
        val taskFile: Path = GROUP_DIR.resolve(task.name() + ".json")
        Files.writeString(taskFile, JSONObject(task).toString(4))

        logger.debug("Preparing Templates")

        Node.templateProvider.prepareTemplate(task.templates.toTypedArray())

        logger.debug("Adding to Groups")

        Node.taskProvider.tasks()!!.add(task)
    }

    fun readGroups(): Set<Task> {
        val groups: HashSet<Task> = HashSet()

        if (!Files.exists(GROUP_DIR)) {
            return groups
        }

        for (file in Objects.requireNonNull(GROUP_DIR.toFile().listFiles())) {
            val taskInformation = JSONObject(Files.readString(file.toPath()))
            val versionJson = taskInformation.getJSONObject("version")
            val version = VersionInfo(
                versionJson.getString("name"),
                VersionType.valueOf(versionJson.get("type").toString()),
                versionJson.getString("versions")
            )
            val nodesJson = taskInformation.getJSONArray("nodes")
            val nodes: Array<String?> = Array(nodesJson.length()) {
                if (nodesJson.isNull(it)) null else nodesJson.getString(it)
            }
            val templatesJson = taskInformation.getJSONArray("nodes")
            val templates: Array<String?> = Array(templatesJson.length()) {
                if (templatesJson.isNull(it)) null else templatesJson.getString(it)
            }

            groups.add(
                TaskImpl(
                    taskInformation.getString("name"),
                    taskInformation.getInt("maxMemory"),
                    version,
                    templates.toList(),
                    nodes.toList(),
                    taskInformation.getInt("maxPlayers"),
                    taskInformation.getBoolean("staticServices"),
                    taskInformation.getInt("minOnlineCount"),
                    taskInformation.getBoolean("maintenance"),
                    taskInformation.getInt("startPort"),
                    taskInformation.getBoolean("fallback")
                )
            )
        }
        return groups
    }

}