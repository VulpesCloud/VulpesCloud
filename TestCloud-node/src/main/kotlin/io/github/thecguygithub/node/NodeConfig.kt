package io.github.thecguygithub.node


import io.github.thecguygithub.node.config.LogLevels
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
    var logLevel: LogLevels? = null
    var mysql_password: String? = null
    var mysql_user: String? = null
    var mysql_host: String? = null
    var mysql_port: Int? = null
    var mysql_use_ssl: Boolean? = null
    var mysql_database: String? = null

    init {
        this.clusterId = "testCloud";
        this.clusterToken = StringUtils.randomString(8)
        //this.localNode = NodeEndpointData("node-" + StringUtils.randomString(4), "127.0.0.1", 9090)
        this.localNode = "Node-1"
        this.redis = RedisEndpointData("default", "0.0.0.0", 6379, "password",)
        this.logLevel = LogLevels.INFO

        this.mysql_port = 25565
        this.mysql_host = "127.0.0.1"
        this.mysql_user = "cloud-system"
        this.mysql_password = "pw"
        this.mysql_use_ssl = false
        this.mysql_database = "cloud-system"

    }
}