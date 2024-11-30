package de.vulpescloud.launcher.dependency

enum class Repository(private val repo: String) {
    MAVEN_CENTRAL("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar");

    val repository: String = repo
}