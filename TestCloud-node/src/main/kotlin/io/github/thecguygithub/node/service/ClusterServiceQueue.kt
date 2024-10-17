package io.github.thecguygithub.node.service

import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.cluster.NodeSituation
import io.github.thecguygithub.node.logging.Logger
import lombok.SneakyThrows
import java.util.*


class ClusterServiceQueue : Thread("cluster-node-service-queue") {
    @SneakyThrows
    override fun run() {
        val taskProvider = Node.taskProvider!!
        val localNode = Node.nodeConfig?.localNode

        Logger().debug(Node.nodeStatus)

        while (Node.nodeStatus != NodeSituation.STOPPING ) {
            Logger().debug("In while loop")

            for (task in taskProvider.groups()!!) {
                Logger().debug("uhhh")
                if (Arrays.stream(task.nodes()).noneMatch { it ->
                        it.equals(
                            localNode,
                            true
                        )
                    }) {
                    continue
                }
                Logger().debug("okay")

                Logger().debug(task.name())
                Logger().debug(task.startPort())

                Logger().debug(task.minOnlineCount())
                if (task.services()?.size!! > 0) {
                    Logger().debug("ALARM!!!!!")

                    val differenceGroupServices = task.minOnlineCount() - task.serviceCount().toInt()

                    Logger().debug("yay")

                    for (i in 0 until differenceGroupServices) {
                        Node.serviceProvider?.factory()?.runGroupService(task)
                    }

                    Logger().debug("WOOO")
                }

            }
            sleep(1000)
        }
    }
}