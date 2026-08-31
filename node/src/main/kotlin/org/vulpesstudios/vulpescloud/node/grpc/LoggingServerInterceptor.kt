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

package org.vulpesstudios.vulpescloud.node.grpc

import io.grpc.*
import org.slf4j.LoggerFactory

class LoggingServerInterceptor : ServerInterceptor {
    private val log = LoggerFactory.getLogger(LoggingServerInterceptor::class.java)
    private val ignoredStatuses = setOf(Status.Code.CANCELLED, Status.Code.UNAVAILABLE)

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val method = call.methodDescriptor.fullMethodName
        val forwardingCall =
            object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                override fun close(status: Status, trailers: Metadata) {
                    when {
                        status.isOk -> Unit
                        ignoredStatuses.contains(status.code) ->
                            log.debug("gRPC <{}> ended with {}", method, status)
                        else -> log.error("gRPC <$method> failed: $status")
                    }
                    super.close(status, trailers)
                }
            }
        val listener = next.startCall(forwardingCall, headers)
        return object :
            ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (t: Throwable) {
                    log.error("Unhandled exception in <$method>", t)
                    call.close(Status.INTERNAL.withDescription(t.message).withCause(t), Metadata())
                }
            }
        }
    }
}
