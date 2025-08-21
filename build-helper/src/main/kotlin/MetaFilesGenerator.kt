import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

fun Project.exportDependenciesJson(
    fileName: String = "dependencies.json",
    ignoredDependencyGroups: Array<String> = emptyArray()
): String {
    val depsArray = JSONArray()

    val outputFolder = layout.buildDirectory.dir("libs").get().asFile

    if (!outputFolder.exists()) {
        outputFolder.mkdirs()
    }

    configurations.getByName("compileClasspath")
        .resolvedConfiguration
        .resolvedArtifacts
        .forEach { artifact ->
            val id = artifact.moduleVersion.id
            if (id.group == group || ignoredDependencyGroups.contains(id.group)) return@forEach

            var resolvedVersion = id.version
            if (id.version.endsWith("-SNAPSHOT") && artifact.id.componentIdentifier is MavenUniqueSnapshotComponentIdentifier) {
                resolvedVersion = (artifact.id.componentIdentifier as MavenUniqueSnapshotComponentIdentifier).timestampedVersion
            }

            val classifierSuffix = artifact.classifier?.let { "-$it" } ?: ""
            val jarFileName = "${id.name}-$resolvedVersion$classifierSuffix.jar"
            val repoPath = "${id.group.replace('.', '/')}/${id.name}/${id.version}/$jarFileName"

            val repo = resolveRepository(repoPath, mavenRepositories())
                ?: throw IllegalStateException("Unable to resolve repository for $id")

            val jarUrl = repo.url.resolve(repoPath).toString()

            val jsonDep = JSONObject()
                .put("group", id.group)
                .put("artifact", id.name)
                .put("version", id.version)
                .put("url", jarUrl)
            artifact.classifier?.let { jsonDep.put("classifier", it) }

            depsArray.put(jsonDep)
        }

    val root = JSONObject().put("dependencies", depsArray)
    println("Dependencies JSON: $root")
    val target = layout.buildDirectory.file(fileName).get().asFile
    target.writeText(root.toString(2))
    return target.absolutePath
}

private fun Project.resolveRepository(
    testUrlPath: String,
    repositories: Iterable<MavenArtifactRepository>
): MavenArtifactRepository? {
    return repositories.firstOrNull { repo ->
        runCatching {
            val url = repo.url.resolve(testUrlPath).toURL()
            (url.openConnection() as HttpURLConnection).run {
                useCaches = false
                readTimeout = 30000
                connectTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Gradle-Dependency-Resolver")
                connect()
                responseCode == 200
            }
        }.getOrElse { false }
    }
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