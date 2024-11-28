package de.vulpescloud.launcher.dependency

import org.jetbrains.annotations.Contract

class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String
) {

    fun downloadUrl(): String {
        return String.format(
            Repository.MAVEN_CENTRAL.repository,
            groupId.replace(".", "/"), artifactId, version, artifactId
        )
    }

    @Contract(pure = true)
    override fun toString(): String {
        return "$artifactId-$version"
    }
}