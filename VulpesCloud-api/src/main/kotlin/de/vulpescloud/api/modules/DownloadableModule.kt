package de.vulpescloud.api.modules

data class DownloadableModule(
    val name: String,
    val version: String,
    val installURL: String,
    val authors: MutableList<Any>,
    val description: String,
    val website: String,
    val supportURL: String
)
