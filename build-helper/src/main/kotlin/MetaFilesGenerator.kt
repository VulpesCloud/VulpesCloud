/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

fun Project.exportDependenciesJson(
    fileName: String = "dependencies.json",
    ignoredDependencyGroups: Array<String> = emptyArray(),
): String {
    val depsArray = JSONArray()
    val outputFolder = layout.buildDirectory.dir("libs").get().asFile
    if (!outputFolder.exists()) outputFolder.mkdirs()

    if (File(outputFolder, fileName).exists()) {
        Files.delete(Path.of(outputFolder.toString(), fileName))
    }

    configurations.getByName("compileClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
        artifact ->
        val id = artifact.moduleVersion.id
        if (id.group == group || ignoredDependencyGroups.contains(id.group)) return@forEach

        // Snapshot-Version handling
        val resolvedVersion =
            if (
                id.version.endsWith("-SNAPSHOT") &&
                    artifact.id.componentIdentifier is MavenUniqueSnapshotComponentIdentifier
            ) {
                (artifact.id.componentIdentifier as MavenUniqueSnapshotComponentIdentifier)
                    .timestampedVersion
            } else id.version

        val classifierSuffix = artifact.classifier?.let { "-$it" } ?: ""

        // Repo URL ermitteln
        val repoUrl =
            if (id.group.startsWith("build.buf")) {
                "https://buf.build/gen/maven/"
            } else null

        val jarUrl =
            repoUrl?.let {
                "$it${id.group.replace('.', '/')}/${id.name}/$resolvedVersion/${id.name}-$resolvedVersion$classifierSuffix.jar"
            }
                ?: resolveArtifactUrl(
                    mavenRepositories().toList(),
                    id.group,
                    id.name,
                    resolvedVersion,
                    classifierSuffix,
                )

        val jsonDep =
            JSONObject()
                .put("group", id.group)
                .put("artifact", id.name)
                .put("version", id.version)
                .put("url", jarUrl)
        artifact.classifier?.let { jsonDep.put("classifier", it) }

        depsArray.put(jsonDep)
    }

    val root = JSONObject().put("dependencies", depsArray)
    val target = layout.buildDirectory.file(fileName).get().asFile
    target.writeText(root.toString(2))
    return target.absolutePath
}

fun Project.mavenRepositories(): Iterable<MavenArtifactRepository> =
    repositories.filterIsInstance<MavenArtifactRepository>()

fun generateCheckSums(destPath: Path, jars: List<String>) {
    val checksumJson = JSONObject()

    jars.forEach { jar ->
        checksumJson.put(jar.removeSuffix(".jar"), getFileChecksum(destPath.resolve(jar).toFile()))
    }

    Files.writeString(destPath.resolve("checksums.json"), checksumJson.toString(4))
}

private fun getFileChecksum(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun resolveArtifactUrl(
    repos: List<MavenArtifactRepository>,
    group: String,
    name: String,
    version: String,
    classifierSuffix: String,
): String {
    val path = "${group.replace('.', '/')}/$name/$version/$name-$version$classifierSuffix.jar"
    for (repo in repos) {
        val base = repo.url.toString().trimEnd('/')
        val candidate = "$base/$path"
        if (checkUrlExists(candidate, repo)) {
            return candidate
        }
    }
    return "" // nothing matched — worth logging/warning
}

private fun checkUrlExists(urlStr: String, repo: MavenArtifactRepository): Boolean {
    return try {
        val connection = URI(urlStr).toURL().openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        // Only MavenArtifactRepository with username/password creds support this cast
        runCatching {
            val creds = repo.credentials
            val user = creds.username
            val pass = creds.password
            if (user != null && pass != null) {
                val auth = java.util.Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
                connection.setRequestProperty("Authorization", "Basic $auth")
            }
        }

        connection.responseCode in 200..299
    } catch (e: Exception) {
        false
    }
}
