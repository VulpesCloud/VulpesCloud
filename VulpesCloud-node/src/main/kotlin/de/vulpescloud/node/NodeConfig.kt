package de.vulpescloud.node


import de.vulpescloud.api.cluster.NodeInformation
import de.vulpescloud.node.config.LogLevels
import de.vulpescloud.node.config.RedisEndpointData
import de.vulpescloud.node.networking.mysql.MySQLEndpointData
import java.util.*

class NodeConfig {

    var uuid: UUID
    var name: String
    var redis: RedisEndpointData?
    var logLevel: LogLevels?
    var mysql: MySQLEndpointData?
    var nodes: MutableList<NodeInformation>


    init {
        this.uuid = UUID.randomUUID()
        this.name = "Node-1"
        this.redis = RedisEndpointData("default", "0.0.0.0", 6379, "password")
        this.logLevel = LogLevels.INFO
        this.mysql = MySQLEndpointData("vulpescloud", "", "vulpescloud", "127.0.0.1", 3306, false)
        this.nodes = mutableListOf()
    }
}