package io.github.thecguygithub.launcher.dependency


class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    var subVersion: String? = null,
    private val repository: Repository = Repository.MAVEN_CENTRAL,
    val withSubDependencies: Boolean = false
) {
    val subDependencies: MutableList<Dependency> = mutableListOf()

    constructor(groupId: String, artifactId: String, version: String) :
            this(groupId, artifactId, version, null, Repository.MAVEN_CENTRAL, false)

    constructor(groupId: String, artifactId: String, version: String, withSubDependencies: Boolean) :
            this(groupId, artifactId, version, null, Repository.MAVEN_CENTRAL, withSubDependencies)

    constructor(groupId: String, artifactId: String, version: String, subVersion: String?, repository: Repository) :
            this(groupId, artifactId, version, subVersion, repository, false)

    fun downloadUrl(): String {
        return repository.repository().format(
            groupId.replace(".", "/"),
            artifactId,
            version,
            artifactId,
            subVersion ?: version
        )
    }

    fun loadSubDependencies() {
        subDependencies.addAll(SubDependencyHelper.findSubDependencies(this))
    }

    override fun toString(): String {
        return "$artifactId-$version"
    }
}