package de.vulpescloud.node.version

import de.vulpescloud.api.version.Environments
import de.vulpescloud.api.version.VersionType
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class VersionProvider {

    private val VERSIONS_URL = "https://raw.githubusercontent.com/VulpesCloud/VulpesCloud-meta/refs/heads/main/versions.json"
    private val VERSIONS_PATH = Path.of("local/versions.json")
    private val logger = LoggerFactory.getLogger(Version::class.java)
    var versions: MutableList<Version> = mutableListOf()

    init {
        Files.writeString(VERSIONS_PATH, String(URI(VERSIONS_URL).toURL().openStream().readAllBytes()))
    }

    fun initialize() {
        val json = JSONObject(Files.readString(VERSIONS_PATH))

        val versionArray = json.getJSONArray("versions")

        for (i in 0 until versionArray.length()) {
            val jsonObject = versionArray.getJSONObject(i)
            val name = jsonObject.getString("name")
            val versionType = jsonObject.getString("versionType")
            val versionsArray = jsonObject.getJSONArray("versions")

            val versions: MutableList<de.vulpescloud.api.version.Version> = mutableListOf()

            for (j in 0 until versionsArray.length()) {
                val versionObject = versionsArray.getJSONObject(j)
                val version = versionObject.getString("version")
                val link = versionObject.getString("link")

                versions.add(de.vulpescloud.api.version.Version(version, link))
            }

            if (name == "Purpur" || name == "Paper") {
                this.versions.add(
                    Version(
                        Environments.valueOf(name.uppercase(Locale.getDefault())),
                        VersionType.valueOf(versionType),
                        "plugins",
                        versions,
                        listOf() // todo Implement Paper Patcher!
                    )
                )
            } else {
                this.versions.add(
                    Version(
                        Environments.valueOf(name.uppercase(Locale.getDefault())),
                        VersionType.valueOf(versionType),
                        "plugins",
                        versions,
                        listOf()
                    )
                )
            }
            logger.debug("Loading Version: $name")
        }
        logger.info("Loading ${versions.size} Versions!") //todo Add translations
    }

    fun getByName(name: String): Version? {
        return versions.stream().filter { it.environment.name.equals(name, true) }.findFirst().orElse(null)
    }

}