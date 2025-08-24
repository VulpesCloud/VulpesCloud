package de.vulpescloud.node.services

import com.google.protobuf.Timestamp
import de.vulpescloud.api.services.ServiceStates
import de.vulpescloud.api.tasks.Task
import java.util.UUID

interface AbstractService {
    val task: Task
    val uuid: UUID
    val orderedId: Int
    val port: Int
    val node: String
    val playerCount: Int
    val startTime: Timestamp
    val state: ServiceStates

    fun start()

    fun stop()

    fun delete()

    fun command(command: String)

    fun restart()



}
