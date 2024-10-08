package io.github.thecguygithub.node.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.node.platforms.PlatformPatcherTypeAdapter
import io.github.thecguygithub.node.platforms.patcher.PlatformPatcher
import io.github.thecguygithub.node.platforms.versions.PlatformVersion
import io.github.thecguygithub.node.platforms.versions.PlatformVersionTypeAdapter


object JsonUtils {

    val GSON: Gson = GsonBuilder().setPrettyPrinting()
        // .registerTypeAdapter(PropertiesPool::class.java, PropertiesPoolSerializer())
        // .registerTypeAdapter(ClusterTask::class.java, ClusterGroupTypeAdapter())
        // .registerTypeAdapter(ClusterGroupFallbackImpl::class.java, ClusterGroupTypeAdapter())
        .registerTypeAdapter(PlatformVersion::class.java, PlatformVersionTypeAdapter())
        .registerTypeAdapter(PlatformPatcher::class.java, PlatformPatcherTypeAdapter())
        .create()

}