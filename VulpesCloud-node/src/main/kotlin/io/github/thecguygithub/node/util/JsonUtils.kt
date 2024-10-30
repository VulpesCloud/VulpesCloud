package io.github.thecguygithub.node.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder


object JsonUtils {

    val GSON: Gson = GsonBuilder().setPrettyPrinting()
        // .registerTypeAdapter(PropertiesPool::class.java, PropertiesPoolSerializer())
//        .registerTypeAdapter(ClusterTask::class.java, ClusterTaskTypeAdapter())
//        .registerTypeAdapter(ClusterTaskFallbackImpl::class.java, ClusterTaskTypeAdapter())
//        .registerTypeAdapter(PlatformVersion::class.java, PlatformVersionTypeAdapter())
//        .registerTypeAdapter(PlatformPatcher::class.java, PlatformPatcherTypeAdapter())
        .create()

}