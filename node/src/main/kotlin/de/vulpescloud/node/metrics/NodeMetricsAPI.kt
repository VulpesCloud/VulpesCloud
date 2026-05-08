package de.vulpescloud.node.metrics

import build.buf.gen.vulpescloud.modules.metrics.v1.*
import build.buf.gen.vulpescloud.modules.metrics.v1.StatisticsServiceGrpcKt
import de.vulpescloud.node.Node
import de.vulpescloud.node.grpc.security.AuthClientInterceptor

object NodeMetricsAPI {

    val metricsStub: StatisticsServiceGrpcKt.StatisticsServiceCoroutineStub by lazy {
        StatisticsServiceGrpcKt.StatisticsServiceCoroutineStub(
                Node.instance.localGrpcClient.channel
            )
            .withInterceptors(AuthClientInterceptor(Node.instance.secret))
    }

    suspend fun write(bucket: String, vararg points: Point) {
        metricsStub.write(
            writeRequest {
                this.bucket = bucket
                this.points += points.toList()
            }
        )
    }

    suspend fun query(bucket: String, fluxQuery: String): QueryResponse {
        return metricsStub.query(queryRequest { query = fluxQuery })
    }

    fun queryStream(bucket: String, fluxQuery: String) =
        metricsStub.queryStream(queryRequest { query = fluxQuery })

    suspend fun delete(bucket: String, startNanos: Long, stopNanos: Long, predicate: String = "") {
        metricsStub.delete(
            deleteRequest {
                this.bucket = bucket
                start = startNanos
                stop = stopNanos
                this.predicate = predicate
            }
        )
    }

    suspend fun health() = metricsStub.health(com.google.protobuf.Empty.getDefaultInstance())

    suspend fun ping() = metricsStub.ping(com.google.protobuf.Empty.getDefaultInstance())
}
