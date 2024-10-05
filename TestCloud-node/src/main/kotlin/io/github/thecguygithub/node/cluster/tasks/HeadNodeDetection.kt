package io.github.thecguygithub.node.cluster.tasks

import io.github.thecguygithub.node.cluster.NodeEndpoint
import io.github.thecguygithub.node.cluster.NodeSituation
import java.util.concurrent.CompletableFuture

object HeadNodeDetection {

    fun detect(service: ClusterProviderImpl): NodeEndpoint {
        if (service.endpoints().isEmpty()) {
            return service.localNode()
        }

        for (endpoint in service.endpoints()) {
            if (endpoint.situation() == NodeSituation.RUNNING) {
                val futureStep = CompletableFuture<String>()

                // Request head node data
                endpoint.transmit()?.request("node-head-request", NodeHeadRequestPacket::class.java) { response ->
                    futureStep.complete(response.headNode())
                }

                val result = futureStep.join()
                val nodeEndpoint = service.find(result)

                if (nodeEndpoint.situation() != NodeSituation.RUNNING || nodeEndpoint.transmit() == null) {
                    // TODO: Handle the problem here
                }
                return nodeEndpoint
            }
        }

        return service.localNode()
    }
}