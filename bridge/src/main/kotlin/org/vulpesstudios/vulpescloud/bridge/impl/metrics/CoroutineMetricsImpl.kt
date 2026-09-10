/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.bridge.impl.metrics

import build.buf.gen.vulpescloud.modules.metrics.v1.*
import com.google.protobuf.Empty
import org.vulpesstudios.vulpescloud.wrapper.Wrapper
import org.vulpesstudios.vulpescloud.wrapper.grpc.AuthClientInterceptor

class CoroutineMetricsImpl {

    val metricsStub: StatisticsServiceGrpcKt.StatisticsServiceCoroutineStub by lazy {
        StatisticsServiceGrpcKt.StatisticsServiceCoroutineStub(Wrapper.instance.grpcClient.channel)
            .withInterceptors(AuthClientInterceptor(System.getenv("secret")))
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

    suspend fun health() = metricsStub.health(Empty.getDefaultInstance())

    suspend fun ping() = metricsStub.ping(Empty.getDefaultInstance())
}
