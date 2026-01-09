package de.vulpescloud.api.cluster

data class NodeCloudVersion(
    val fullVersion: String,
    val version: String,
    val buildNumber: Int,
    val gitBranch: String,
    val gitCommit: String
)