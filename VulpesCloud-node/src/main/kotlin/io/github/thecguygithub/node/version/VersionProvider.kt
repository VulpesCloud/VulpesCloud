package io.github.thecguygithub.node.version

import io.github.thecguygithub.api.version.VersionType
import io.github.thecguygithub.api.version.Versions
import io.github.thecguygithub.node.logging.Logger
import io.github.thecguygithub.node.networking.redis.RedisJsonParser
import io.github.thecguygithub.node.util.StringUtils
import io.github.thecguygithub.node.version.files.strat.PaperStrategy
import io.github.thecguygithub.node.version.patcher.PaperPatcher
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class VersionProvider {

    var FORWARDING_SECRET: String = StringUtils.randomString(8)

    private val VERSIONS_URL =
        "https://raw.githubusercontent.com/VulpesCloud/VulpesCloud-meta/refs/heads/main/versions.json"
    private val VERISON_PATH = Path.of("local/versions.json")

    val logger = Logger()
    var versions: MutableList<Version> = mutableListOf()

    init {

        // Files.writeString(VERISON_PATH, String(URI(VERSIONS_URL).toURL().openStream().readAllBytes()))

        val json = RedisJsonParser.parseJson(Files.readString(VERISON_PATH))

        val versionArray = json.getJSONArray("versions")

        for (i in 0 until versionArray.length()) {
            val versionObject = versionArray.getJSONObject(i)
            val name = versionObject.getString("name")
            val versionType = versionObject.getString("versionType")
            val versionsArray = versionObject.getJSONArray("versions")

            val vers: MutableList<Versions> = mutableListOf()

            for (j in 0 until versionsArray.length()) {

                val ver = versionsArray.getJSONObject(j)
                val version = ver.getString("version")
                val link = ver.getString("link")

                vers.add(Versions(version, link))

            }

            if (name == "Purpur" || name == "Paper") {
                versions.add(
                    Version(name, VersionType.valueOf(versionType), "plugins", listOf("-nogui"), listOf(PaperPatcher()), listOf(), vers)
                )
            } else {
                versions.add(
                    Version(name, VersionType.valueOf(versionType), "plugins", listOf("-nogui"), listOf(), listOf(), vers)
                )
            }

        }

        logger.info("Loading ${versions.size} Versions")

    }

    fun search(name: String): Version? {
        return versions.stream().filter { it.name.equals(name, true) }.findFirst().orElse(null)
    }



}