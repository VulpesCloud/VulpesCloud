package de.vulpescloud.node.version

import de.vulpescloud.api.version.SingleVersion
import de.vulpescloud.api.version.Version
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.api.version.VersionType
import org.json.JSONObject
import java.net.URI
import java.nio.file.Files
import kotlin.io.path.Path

class VersionProviderImpl : VersionProvider {

    private val versions = mutableListOf<Version>()
    private val versionURL = "https://raw.githubusercontent.com/VulpesCloud/VulpesCloud-meta/refs/heads/main/versions.json"
    private val versionsPath = Path("local/versions.json")

    override fun getVersionByName(name: String): Version? {
        return versions.find { it.name == name }
    }

    override fun getVersionsByType(type: VersionType): List<Version> {
        return versions.filter { it.type == type }
    }

    override fun getAllRegisteredVersions(): List<Version> {
        return versions
    }

    override fun registerCustomVersion(version: Version) {
        versions.add(version)
    }

    fun initialize() {
        val json = JSONObject(String(URI(versionURL).toURL().openStream().readAllBytes())).getJSONArray("versions")

        for (i in 0 until json.length()) {
            val jsonObject = json.getJSONObject(i)
            val name = jsonObject.getString("name")
            val versionType = jsonObject.getString("type")
            val versionsArray = jsonObject.getJSONArray("versions")
            val pluginDir = jsonObject.getString("pluginDir")

            val version = mutableListOf<SingleVersion>()

            for (j in 0 until versionsArray.length()) {
                val versionObject = versionsArray.getJSONObject(j)
                val versionName = versionObject.getString("version")
                val downloadURL = versionObject.getString("url")
                version.add(SingleVersion(versionName, downloadURL))
            }
            
            versions.add(
                Version(
                    name,
                    VersionType.valueOf(versionType),
                    pluginDir,
                    version
                )
            )
        }

    }

}
