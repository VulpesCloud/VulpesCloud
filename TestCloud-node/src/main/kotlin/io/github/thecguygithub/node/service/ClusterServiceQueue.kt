package io.github.thecguygithub.node.service

import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.cluster.NodeSituation
import lombok.SneakyThrows
import java.util.*


class ClusterServiceQueue : Thread("cluster-node-service-queue") {
    @SneakyThrows
    override fun run() {
        val taskProvider = Node.taskProvider!!
        val localNode = Node.nodeConfig?.localNode

        while (Node.nodeStatus != NodeSituation.STOPPING ) {

            for (task in taskProvider.groups()!!) {
                if (Arrays.stream(task.nodes()).noneMatch { it ->
                        it.equals(
                            localNode,
                            true
                        )
                    }) {
                    continue
                }

                val differenceGroupServices = task.minOnlineCount() - task.serviceCount()

                for (i in 0 until differenceGroupServices) {
                    Node.serviceProvider?.factory()?.runGroupService(task)
                }

            }
            sleep(1000)
        }
    }
}