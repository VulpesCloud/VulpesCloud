package io.github.thecguygithub.node.platforms.patcher

import io.github.thecguygithub.node.service.ClusterLocalServiceImpl
import java.io.File


interface PlatformPatcher {
    fun patch(serverFile: File?, clusterLocalService: ClusterLocalServiceImpl?)

    fun id(): String?
}