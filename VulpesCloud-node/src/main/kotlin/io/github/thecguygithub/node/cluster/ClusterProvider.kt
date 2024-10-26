package io.github.thecguygithub.node.cluster

import io.github.thecguygithub.api.cluster.NodeInformation
import io.github.thecguygithub.api.cluster.NodeStates
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.logging.Logger
import java.util.*
import javax.sql.rowset.RowSetFactory
import javax.sql.rowset.RowSetProvider


class ClusterProvider {

    private val redis = Node.instance!!.getRC()
    private val mySQL = Node.mySQLController
    private val logger = Logger()
    private lateinit var rowSetFactory: RowSetFactory
    val nodes: MutableList<NodeInformation> = mutableListOf()

    companion object {
        lateinit var instance: ClusterProvider
    }


    init {

        instance = this

        try {
            rowSetFactory = RowSetProvider.newFactory()
        } catch (e: Exception) {
            logger.error(e)
        }

        val redisNodes = redis?.getAllHashFields("VulpesCloud-Nodes")

        if (redisNodes.isNullOrEmpty()) {

            try {
                val result = mySQL.dbExecWithRs("SELECT * FROM nodes")
                val cachedRowSet = rowSetFactory.createCachedRowSet()
                cachedRowSet.populate(result)
                val resultMeta = cachedRowSet.metaData

                while (cachedRowSet.next()) {
                    nodes.add(
                        NodeInformation(
                            cachedRowSet.getString(2),
                            UUID.fromString(cachedRowSet.getString(3))
                        )
                    )
                }

                if (nodes.size == 0) {
                    logger.warn("The nodes Table in the Database is empty!")
                    logger.warn("Setting the Table to what is set in the config.")
                    logger.warn("Please check if you have configured the right Database next time! :/")

                    mySQL.dbExecute("INSERT INTO nodes (name, uuid) VALUES ('${Node.nodeConfig!!.name}', '${Node.nodeConfig!!.uuid}')")

                    Node.nodeConfig!!.nodes.forEach { mySQL.dbExecute("INSERT INTO nodes (name, uuid) VALUES ('${it.name}', '${it.uuid}')") }

                    val res = mySQL.dbExecWithRs("SELECT * FROM nodes")
                    val crs = rowSetFactory.createCachedRowSet()
                    crs.populate(res)

                    while (crs.next()) {
                        nodes.add(
                            NodeInformation(
                                crs.getString(2),
                                UUID.fromString(crs.getString(3))
                            )
                        )
                    }
                }
//
//                if (Node.nodeConfig!!.nodes != nodes) {
//                    logger.warn("The Nodes Configuration is not what is stored in the MySQL Database!")
//                    logger.warn("Please do NOT remove through the Config! Use The cluster Command instead!")
//                    logger.warn("The Config will be set to the Data in the Database!")
//
//                    Node.nodeConfig!!.nodes = nodes
//                    Node.nodeConfig!!.nodes.remove(NodeInformation(Node.nodeConfig!!.name, Node.nodeConfig!!.uuid))
//                    Node.instance!!.updateConfig()
//                }

                nodes.forEach { redis?.setHashField("VulpesCloud-Nodes", it.name, NodeStates.OFFLINE.name) }

                updateLocalNodeState(NodeStates.STARTING)

            } catch (e: Exception) {
                logger.error(e)
            }
        }
    }


    fun updateLocalNodeState(state: NodeStates) {
        redis?.setHashField("VulpesCloud-Nodes", Node.nodeConfig!!.name, state.name)
        redis?.sendMessage("NODE;${Node.nodeConfig!!.name};STATE;${state.name}", "vulpescloud-event-node-state")
    }


    fun findByName(node: String) {}

}