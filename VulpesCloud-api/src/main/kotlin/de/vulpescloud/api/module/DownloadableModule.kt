package de.vulpescloud.api.module

data class DownloadableModule(
    val name: String,
    val version: String,
    val installURL: String,
    val authors: MutableList<String>,
    val description: String,
    val website: String,
    val supportURL: String,
)
