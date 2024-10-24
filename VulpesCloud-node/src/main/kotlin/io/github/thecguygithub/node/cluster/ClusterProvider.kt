package io.github.thecguygithub.node.cluster

import io.github.thecguygithub.api.cluster.NodeInformation
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


    init {

        try {
            rowSetFactory = RowSetProvider.newFactory()
        } catch (e: Exception) {
            logger.error(e)
        }

        val redisNodes = redis?.getAllHashFields("VulpesCloud-Nodes")

        if (redisNodes == null) {

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

                nodes.forEach { logger.debug(it.name + "Name | UUID" + it.uuid) }

            } catch (e: Exception) {
                logger.error(e)
            }

        }

    }

}