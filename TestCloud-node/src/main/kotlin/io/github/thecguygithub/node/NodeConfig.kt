package io.github.thecguygithub.node


import io.github.thecguygithub.node.config.RedisEndpointData
import io.github.thecguygithub.node.util.StringUtils
import lombok.Getter
import lombok.Setter
import lombok.experimental.Accessors


@Getter
@Setter
@Accessors(fluent = true)
class NodeConfig {
    var clusterId: String = null.toString()
    var clusterToken: String
    var localNode: String = null.toString()
    var redis: RedisEndpointData? = null
    // private var nodes: Set<NodeEndpointData>

    // private var propertiesPool: PropertiesPool

    init {
        this.clusterId = "testCloud";
        this.clusterToken = StringUtils.randomString(8)
        //this.localNode = NodeEndpointData("node-" + StringUtils.randomString(4), "127.0.0.1", 9090)
        this.localNode = "Node-1"
        this.redis = RedisEndpointData("default", "0.0.0.0", 6379, "password",)
        // this.nodes = HashSet<NodeEndpointData>()

        // this.propertiesPool = PropertiesPool()
    }
}