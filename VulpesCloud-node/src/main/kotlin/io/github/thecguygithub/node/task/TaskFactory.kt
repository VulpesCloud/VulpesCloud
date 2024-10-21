package io.github.thecguygithub.node.task

import io.github.thecguygithub.node.logging.Logger
import org.json.JSONObject
import java.nio.file.Path

object TaskFactory {

    private val GROUP_DIR = Path.of("local/groups")
    private val logger = Logger()

    fun createNewTask(taskInformation: JSONObject) {

        val platformJson = taskInformation.getJSONObject("platformInformation")

    }

}