package io.github.thecguygithub.launcher.dependency

enum class Repository(private val repository: String) {

    MAVEN_CENTRAL("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar"),
    MAVEN_CENTRAL_SNAPSHOT("https://s01.oss.sonatype.org/service/local/repositories/snapshots/content/%s/%s/%s/%s-%s.jar");

    fun repository(): String {
        return repository
    }
}