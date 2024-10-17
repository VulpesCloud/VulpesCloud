package io.github.thecguygithub.node.tasks

import io.github.thecguygithub.api.tasks.ClusterTask
import io.github.thecguygithub.api.tasks.FallbackClusterTask


class ClusterTaskFallbackImpl(group: ClusterTask) : ClusterTaskImpl(
    group.name(),
    group.platform(),
    group.templates(),
    group.nodes(),
    group.maxMemory(),
    group.maxPlayers(),
    group.staticService(),
    group.minOnlineCount(),
    group.maintenance(),
    group.startPort()
),
    FallbackClusterTask