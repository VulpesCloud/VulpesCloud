package io.github.thecguygithub.node.platforms.parser

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.thecguygithub.node.util.JsonUtils
import io.github.thecguygithub.node.util.StringUtils
import lombok.SneakyThrows



object PaperBuildParser {

    init {

    }

    private const val BUILD_URL_TEMPLATE = "https://api.papermc.io/v2/projects/%s/versions/%s/builds"
    private const val DOWNLOAD_URL_TEMPLATE = "https://api.papermc.io/v2/projects/%s/versions/%s/builds/%d/downloads/%s"

    @SneakyThrows
    fun latestBuildUrl(version: String?): String {
        val buildUrl = String.format(BUILD_URL_TEMPLATE, "paper", version)
        val response = JsonUtils.GSON.fromJson(
            StringUtils.downloadStringContext(buildUrl),
            JsonObject::class.java
        )
        val builds: JsonArray? = response.getAsJsonArray("builds")

        if (builds != null) {
            if (builds.isEmpty) {
                return ""
            }
        }

        val latestBuild: JsonObject? = builds?.get(builds.size() - 1)?.getAsJsonObject()
        val buildNumber: Int = latestBuild?.get("build")?.asInt!!
        val downloads = latestBuild.getAsJsonObject("downloads")
        val application: JsonObject? = downloads.getAsJsonObject("application")
        val fileName: String? = application?.get("name")?.asString

        return String.format(DOWNLOAD_URL_TEMPLATE, "paper", version, buildNumber, fileName)
    }

}