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

package de.vulpescloud.node.version

import de.vulpescloud.api.version.VersionType
import de.vulpescloud.api.version.Versions
import de.vulpescloud.node.networking.redis.RedisJsonParser
import de.vulpescloud.node.util.StringUtils
import de.vulpescloud.node.version.patcher.PaperPatcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class VersionProvider {

    var FORWARDING_SECRET: String = StringUtils.randomString(8)

    private val VERSIONS_URL =
        "https://raw.githubusercontent.com/VulpesCloud/VulpesCloud-meta/refs/heads/main/versions.json"
    private val VERISON_PATH = Path.of("local/versions.json")

    val logger: Logger = LoggerFactory.getLogger(VersionProvider::class.java)
    var versions: MutableList<Version> = mutableListOf()

    init {

        Files.writeString(VERISON_PATH, String(URI(VERSIONS_URL).toURL().openStream().readAllBytes()))

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
                    Version(name, VersionType.valueOf(versionType), "plugins", listOf(PaperPatcher()), listOf(), vers)
                )
            } else {
                versions.add(
                    Version(name, VersionType.valueOf(versionType), "plugins", listOf(), listOf(), vers)
                )
            }

        }

        logger.info("Loading ${versions.size} Versions")

    }

    fun search(name: String): Version? {
        return versions.stream().filter { it.name.equals(name, true) }.findFirst().orElse(null)
    }



}