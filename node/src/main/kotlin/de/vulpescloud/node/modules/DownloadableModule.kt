package de.vulpescloud.node.modules

data class DownloadableModule(
    val name: String,
    val version: String,
    val installURL: String,
    val authors: MutableList<String>,
    val description: String,
    val website: String,
    val supportURL: String,
)