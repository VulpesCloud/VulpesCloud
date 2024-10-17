package io.github.thecguygithub.node.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.node.platforms.PlatformPatcherTypeAdapter
import io.github.thecguygithub.node.platforms.patcher.PlatformPatcher
import io.github.thecguygithub.node.platforms.versions.PlatformVersion
import io.github.thecguygithub.node.platforms.versions.PlatformVersionTypeAdapter
import io.github.thecguygithub.node.tasks.ClusterTaskFallbackImpl
import io.github.thecguygithub.node.tasks.ClusterTaskTypeAdapter


object JsonUtils {

    val GSON: Gson = GsonBuilder().setPrettyPrinting()
        // .registerTypeAdapter(PropertiesPool::class.java, PropertiesPoolSerializer())
        .registerTypeAdapter(ClusterTask::class.java, ClusterTaskTypeAdapter())
        .registerTypeAdapter(ClusterTaskFallbackImpl::class.java, ClusterTaskTypeAdapter())
        .registerTypeAdapter(PlatformVersion::class.java, PlatformVersionTypeAdapter())
        .registerTypeAdapter(PlatformPatcher::class.java, PlatformPatcherTypeAdapter())
        .create()

}