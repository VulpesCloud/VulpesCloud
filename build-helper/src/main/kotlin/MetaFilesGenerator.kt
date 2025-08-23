import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

fun Project.exportDependenciesJson(
    fileName: String = "dependencies.json",
    ignoredDependencyGroups: Array<String> = emptyArray()
): String {
    val depsArray = JSONArray()
    val outputFolder = layout.buildDirectory.dir("libs").get().asFile
    if (!outputFolder.exists()) outputFolder.mkdirs()

    if (File(outputFolder, fileName).exists()) {
        Files.delete(Path.of(outputFolder.toString(), fileName))
    }

    configurations.getByName("compileClasspath")
        .resolvedConfiguration
        .resolvedArtifacts
        .forEach { artifact ->
            val id = artifact.moduleVersion.id
            if (id.group == group || ignoredDependencyGroups.contains(id.group)) return@forEach

            // Snapshot-Version handling
            val resolvedVersion = if (
                id.version.endsWith("-SNAPSHOT") &&
                artifact.id.componentIdentifier is MavenUniqueSnapshotComponentIdentifier
            ) {
                (artifact.id.componentIdentifier as MavenUniqueSnapshotComponentIdentifier).timestampedVersion
            } else id.version

            val classifierSuffix = artifact.classifier?.let { "-$it" } ?: ""

            // Repo URL ermitteln
            val repoUrl = if (id.group.startsWith("build.buf")) {
                "https://buf.build/gen/maven/"
            } else {
                // erstes MavenRepo nehmen, das diese Dependency liefert
                repositories.filterIsInstance<MavenArtifactRepository>()
                    .firstOrNull { repo ->
                        // grober Match: prüft, ob groupId zum Repo passt oder sonst nehmen wir das erste Repo
                        true
                    }?.url?.toString() ?: ""
            }

            val jarUrl = "$repoUrl${id.group.replace('.', '/')}/${id.name}/$resolvedVersion/${id.name}-$resolvedVersion$classifierSuffix.jar"

            val jsonDep = JSONObject()
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