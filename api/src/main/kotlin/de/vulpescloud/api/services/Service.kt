package de.vulpescloud.api.services

import build.buf.gen.vulpescloud.services.v1.ServiceDefinition
import build.buf.gen.vulpescloud.services.v1.ServiceState
import com.google.protobuf.Timestamp
import de.vulpescloud.api.serializer.TimestampSerializer
import de.vulpescloud.api.serializer.UUIDSerializer
import de.vulpescloud.api.tasks.Task
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonString
import java.util.*

@Serializable
data class Service(
    val task: Task,
    @Serializable(UUIDSerializer::class) val uuid: UUID,
    val orderedId: Int,
    val port: Int,
    val node: String,
    val playerCount: Int,
    @Serializable(TimestampSerializer::class) val startTime: Timestamp,
    val state: ServiceStates,
    val hostname: String,
) {
    fun toDocument(): BsonDocument =
        BsonDocument().apply {
            append("task", task.toDocument())
            append("uuid", BsonString(uuid.toString()))
            append("orderedId", BsonInt32(orderedId))
            append("port", BsonInt32(port))
            append("node", BsonString(node))
            append("playerCount", BsonInt32(playerCount))
            append("startTime", BsonInt64(startTime.seconds))
            append("state", BsonInt32(state.ordinal))
            append("hostname", BsonString(hostname))
        }

    fun toDefinition(): ServiceDefinition =
        ServiceDefinition.newBuilder()
            .setTask(task.toDefinition())
            .setUuid(uuid.toString())
            .setOrderedId(orderedId)
            .setPort(port)
            .setNode(node)
            .setPlayerCount(playerCount)
            .setStartTime(startTime)
            .setState(
                when (state) {
                    ServiceStates.UNKNOWN -> ServiceState.SERVICE_STATE_UNSPECIFIED
                    ServiceStates.PREPARED -> ServiceState.SERVICE_STATE_PREPARED
                    ServiceStates.STARTING -> ServiceState.SERVICE_STATE_STARTING
                    ServiceStates.RUNNING -> ServiceState.SERVICE_STATE_RUNNING
                    ServiceStates.STOPPED -> ServiceState.SERVICE_STATE_STOPPED
                }
            )
            .setHostname(hostname)
            .build()

    companion object {
        fun fromDefinition(definition: ServiceDefinition): Service {
            return Service(
                Task.fromDefinition(definition.task),
                UUID.fromString(definition.uuid),
                definition.orderedId,
                definition.port,
                definition.node,
                definition.playerCount,
                definition.startTime,
                when (definition.state) {
                    ServiceState.SERVICE_STATE_UNSPECIFIED -> ServiceStates.UNKNOWN
                    ServiceState.SERVICE_STATE_PREPARED -> ServiceStates.PREPARED
                    ServiceState.SERVICE_STATE_STARTING -> ServiceStates.STARTING
                    ServiceState.SERVICE_STATE_RUNNING -> ServiceStates.RUNNING
                    ServiceState.SERVICE_STATE_STOPPED -> ServiceStates.STOPPED
                    ServiceState.UNRECOGNIZED -> ServiceStates.UNKNOWN
                },
                definition.hostname,
            )
        }

        fun fromDocument(document: BsonDocument): Service {
            return Service(
                Task.fromDocument(document.getDocument("task")),
                UUID.fromString(document.getString("uuid").value),
                document.getInt32("orderedId").value,
                document.getInt32("port").value,
                document.getString("node").value,
                document.getInt32("playerCount").value,
                document.getInt64("startTime").value.let {
                    Timestamp.newBuilder().setSeconds(it).build()
                },
                ServiceStates.entries[document.getInt32("state").value],
                document.getString("hostname").value,
            )
        }
    }
}
