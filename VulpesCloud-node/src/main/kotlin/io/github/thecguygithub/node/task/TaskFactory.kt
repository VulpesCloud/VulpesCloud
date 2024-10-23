package io.github.thecguygithub.node.task

import io.github.thecguygithub.api.version.VersionInfo
import io.github.thecguygithub.api.version.VersionType
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

object TaskFactory {

    private val GROUP_DIR = Path.of("local/groups")
    private val logger = Logger()

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


        val templatesJson = taskInformation.getJSONArray("nodes")

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
            taskInformation.getInt("startPort")
        )

        GROUP_DIR.toFile().mkdirs()
        val taskFile: Path = GROUP_DIR.resolve(task.name() + ".json")
        Files.writeString(taskFile, JSONObject(task).toString(4))

        logger.debug("Adding to Groups")

        Node.taskProvider.tasks()!!.add(task)


    }

}