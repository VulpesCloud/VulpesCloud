package de.vulpescloud.node.grpc

import io.grpc.*
import org.slf4j.LoggerFactory

class LoggingServerInterceptor : ServerInterceptor {
    private val log = LoggerFactory.getLogger(LoggingServerInterceptor::class.java)

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val method = call.methodDescriptor.fullMethodName
        val forwardingCall = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                if (!status.isOk) {
                    log.error("gRPC <$method> failed: $status")
                }
                super.close(status, trailers)
            }
        }
        val listener = next.startCall(forwardingCall, headers)
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (t: Throwable) {
                    log.error("Unhandled exception in <$method>", t)
                    call.close(
                        Status.INTERNAL.withDescription(t.message).withCause(t),
                        Metadata()
                    )
                }
            }
        }
    }
}